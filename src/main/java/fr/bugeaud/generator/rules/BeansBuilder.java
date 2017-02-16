package fr.bugeaud.generator.rules;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.poi.ss.formula.eval.ErrorEval;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BOOLEAN;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_FORMULA;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_NUMERIC;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_STRING;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.LocaleUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Generate a list of beans from a spreadsheet document
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class BeansBuilder {
    //public static final String RULE_SHEET_NAME = "Rules";
    public static final int HEADER_ROW_INDEX = 0;
    
    /**
    * Create a list of beans according to its type from the data in the referenced spreadsheet
     * @param <T> the main parametric type matching the bean's type, it will be used using introspection
     * @param in the spreadsheet's stream
     * @param sheetName the spreadsheet's sheet name where the data are located
     * @param theClass the bean's class instance to use as data structure
     * @param aliases a map with key as the property names and value as the matching column name in the spreadsheet. If null, no name mapping is applied. 
     * @return the list of beans
     * @throws IOException if any issue reading the spreadsheet's stream
     * @throws IntrospectionException if any issue with the bean's class
     * @throws IllegalAccessException if issue when trying to acces the bean
     * @throws InstantiationException if failing to instanciate the class
     * @throws IllegalArgumentException if unable to call the bean's accessor
     * @throws InvocationTargetException if any exception was raised during the bean's accessor's call
     */
    public static <T> List<T> createData(InputStream in, String sheetName, Class<T> theClass, Map<String,String> aliases) throws IOException,IntrospectionException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException{        
        XSSFWorkbook workBook = new XSSFWorkbook(in);
        XSSFSheet sheet = workBook.getSheet(sheetName);
        return buildDataList(sheet, theClass,aliases);
    }   
    
    /**
     * Build a Map of PropertyDescriptor for a given class
     * @param klass the target class
     * @return a Map of PropertyDescriptor for a given class
     * @throws IntrospectionException when trying to introspect the
     */
    public static Map<String, PropertyDescriptor> getProperties(Class<?> klass) throws IntrospectionException{
        final PropertyDescriptor[] properties = Introspector.getBeanInfo(klass).getPropertyDescriptors();        
        return Stream.of(properties).collect(Collectors.toMap(PropertyDescriptor::getDisplayName, Function.identity()));        
    }
    
    /**
     * Return the sheet table headers in the order written
     * @param sheet the sheet table
     * @return the list of column name given in the header
     */
    public static List<String> getSheetHeaders(XSSFSheet sheet){
        final List<String> headers = new ArrayList<String>();
        final XSSFRow headerRow = sheet.getRow(HEADER_ROW_INDEX);
        
        // Will load a formater according to the default locale
        final DataFormatter formater = new DataFormatter();
        short lastCell = headerRow.getLastCellNum();
        for(short i=0;i<lastCell;i++){
            XSSFCell cell = headerRow.getCell(i);
            if(cell!=null){
                // Add the cell value alond to its
                //headers.add(Introspector.decapitalize(formater.formatCellValue(cell)));
                headers.add(formater.formatCellValue(cell));
            }            
        }
        return headers;
    }

    
    protected static Object getCellValue(XSSFCell cell) {
        switch (cell.getCellType()) {
            case CELL_TYPE_BLANK:
                return "";
            case CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue();
            case CELL_TYPE_ERROR:
                return cell.getErrorCellValue();
            case CELL_TYPE_FORMULA:
                return cell.getCellFormula();
            case CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {                    
                    return cell.getDateCellValue();
                }
                return new BigDecimal(cell.getCTCell().getV());// getNumericCellValue();
            case CELL_TYPE_STRING:
                return cell.getRichStringCellValue().toString();
            default:
                return "Unknown Cell Type: " + cell.getCellType();
        }
    }
    
    
    protected final static <T> T buildDataInstance(XSSFRow row, Class<T> theClass, List<String> headers, Map<String, PropertyDescriptor> propertiesCache, Map<String, String> aliases)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
        final T newInstance = theClass.newInstance();
        short lastCell = row.getLastCellNum();
        for(short i=0;i<lastCell;i++){
            final XSSFCell cell = row.getCell(i);
            
            if(cell==null){
                // If no value was found in the cell, skip the cell
                continue;
            }
            
            Object value = getCellValue(cell);//cell.getStringCellValue();
            
            if(value==null || "".equals(value)){
                // If null or empty string was found then skip the cell
                continue;
            }
            
            final String columnName = headers.get(i);
            String propertyName = columnName;
            if(aliases!=null){
                // If there is an aliases map set, use it to transform the column names into property's name
                propertyName = aliases.get(propertyName);
                
                // Allow default mapping of properties with column names
                if(propertyName==null){
                    propertyName = columnName;
                }
            }
            if(propertyName!=null){
                // Decapitalize to make it look like a property
                propertyName = Introspector.decapitalize(propertyName);
            }
            
            // Get the matching properties descriptor
            final PropertyDescriptor propertyDescriptor = propertiesCache.get(propertyName);
           
            if(propertyDescriptor==null){
                // If no descriptor was found, simply get out
                continue;
            }
            
            // Check if type is appendable
            // If a list is found, any new value will be added to it
            // If a map is found, any new value will be put with the spreadsheet column name as a key
            if (propertyDescriptor.getPropertyType().isAssignableFrom(List.class)){
                // Fetch the existing value
                List list = (List)propertyDescriptor.getReadMethod().invoke(newInstance);
                if(list==null){
                    // If no list already exists create a news one
                    list = new ArrayList();
                }
                list.add(value);
                propertyDescriptor.getWriteMethod().invoke(newInstance, list);
            }else if(propertyDescriptor.getPropertyType().isAssignableFrom(Map.class)){
                // Fetch the existing value
                Map map = (Map)propertyDescriptor.getReadMethod().invoke(newInstance);
                if(map==null){
                    // If no list already exists create a news one
                    map = new HashMap();
                }
                map.put(columnName,value);
                propertyDescriptor.getWriteMethod().invoke(newInstance, map);
            }else if(propertyDescriptor.getPropertyType().isAssignableFrom(String.class)){
                if(value!=null && ! (value instanceof String)){
                    // If the target is a string property but the parsed value is not, attempt to convert to string
                    value = value.toString();
                }
                propertyDescriptor.getWriteMethod().invoke(newInstance, value);
            }else{                
                propertyDescriptor.getWriteMethod().invoke(newInstance, value);                
            }
            
            
        }

        return newInstance;
    } 

    
    /**
     * Create a list of beans according to its type from the data in the referenced spreadsheet
     * @param <T> the main parametric type matching the bean's type, it will be used using introspection
     * @param sheet the sheet where the data are located
     * @param theClass the bean's class instance to use as data structure
     * @param aliases a map with key as the property names and value as the matching column name in the spreadsheet. If null, no name mapping is applied. 
     * @return the list of beans
     * @throws IntrospectionException if any issue with the bean's class
     * @throws IllegalAccessException if issue when trying to acces the bean
     * @throws InstantiationException if failing to instanciate the class
     * @throws IllegalArgumentException if unable to call the bean's accessor
     * @throws InvocationTargetException if any exception was raised during the bean's accessor's call
     */
    protected static <T> List<T> buildDataList(XSSFSheet sheet, Class<T> theClass, Map<String, String> aliases) throws IntrospectionException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException{
        final List<T> dataList = new ArrayList<>();
        final List<String> headers = getSheetHeaders(sheet);
        final Map<String, PropertyDescriptor> properties = getProperties(theClass);
        
        final Iterator<Row> it = sheet.iterator();
        
        // Assumes it has at least one row and as it is a header, drop it
        it.next();
        
        // Loop on rows to build bean instances
        while(it.hasNext()) {
            final Row r = it.next();
            final XSSFRow xr = (XSSFRow)r;
            T instance = buildDataInstance(xr,theClass,headers,properties,aliases);
            if(instance == null){
                // Let's skip
                continue;
            }
            dataList.add(instance);
        }

        return dataList;
    }
    
}
