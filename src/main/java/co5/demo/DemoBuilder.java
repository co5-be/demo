package co5.demo;

import co5.backflow.client.StageDescriptor;
import co5.backflow.client.Builder;
import co5.backflow.client.LogData;
import co5.backflow.client.Logging;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

enum DemoStages{LOAD_XLSX, RENAME_SHEETS, SET_PAYLOAD};

public class DemoBuilder implements Builder<DemoWorkUnit, DemoStages>{

    Logging _Log = new Logging(null);
    
    public ArrayList<StageDescriptor<DemoWorkUnit, DemoStages>> build(){
        ArrayList<StageDescriptor<DemoWorkUnit, DemoStages>> s = new ArrayList<>();
        s.add(new StageDescriptor<>((byte)4, (byte)1, this::loadExcel, (short)20, (short)10, DemoStages.LOAD_XLSX));
        s.add(new StageDescriptor<>((byte)4, (byte)1, this::renameSheets, (short)20, (short)10, DemoStages.RENAME_SHEETS));
        s.add(new StageDescriptor<>((byte)4, (byte)1, this::setPayload, (short)20, (short)10, DemoStages.SET_PAYLOAD));
        return s;
    }

    public Function<UUID, DemoWorkUnit> getWorkUnitFactory(){
        return this::getWorkUnit;
    }
    
    private DemoWorkUnit getWorkUnit(UUID id){
        return new DemoWorkUnit(false, id);
    }
    
    public Function<String, Boolean> getAuthorizer(){
        return this::auth;
    }
    
    private Boolean auth(String header){
        return true;
    }
    
    private DemoWorkUnit loadExcel(DemoWorkUnit tw){
        DemoWorkUnit ret = tw;
        try {
            tw.Workbook = new XSSFWorkbook(new ByteArrayInputStream(tw.payload));
        } catch (POIXMLException | IOException  ex) {
            _Log.print(new LogData(DemoStages.LOAD_XLSX.name(), tw.id, ex.getMessage()));
            ret = null;
        }
        finally{
            tw.payload = null;
        }
        return ret;
    }

    private DemoWorkUnit renameSheets(DemoWorkUnit tw){        
        DemoWorkUnit ret = tw;
        for (int i = tw.Workbook.getNumberOfSheets() - 1; i > -1; i--){
            tw.Workbook.setSheetName(i, String.format("Renamed%d", i));
        }
        return ret;
    }
    
    private DemoWorkUnit setPayload(DemoWorkUnit tw){
        DemoWorkUnit ret = tw;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            tw.Workbook.write(baos);
            tw.payload = baos.toByteArray();
            ret.httpStatus = 200;
            ret.done = true;
        } catch (IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            ret = null;
        }
        return ret;
    }
}
