package co5.demo;

import co5.backflow.client.WorkUnit;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DemoWorkUnit extends WorkUnit{
    public XSSFWorkbook Workbook;
    public DemoWorkUnit(){
        super();
    }
    
    public DemoWorkUnit(boolean stop, UUID i){
        super(stop, i.toString());
    }
}
