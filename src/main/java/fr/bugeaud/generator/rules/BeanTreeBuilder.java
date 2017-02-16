package fr.bugeaud.generator.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Generate a tree from a bean
 * @author bugeaud at gmail dot com
 * @licence CeCILL 2.1
 */
public class BeanTreeBuilder {
    protected List<String> groups;
        
    public BeanTreeBuilder(List<String> groups){
        this.groups = groups;
    }
    
    public static final String EMPTY_STRING = "";
    
    public static List<Rule> clean(List<Rule> beans){
        return clean(beans, EMPTY_STRING);
    }
    
    public static List<Rule> clean(List<Rule> beans, String defaultCategory){
        beans.stream().filter((r) -> (r.getCategory()==null)).forEach((r) -> {
            r.setCategory(defaultCategory);
        });
        return beans;
    }
    
    public static Map<String,List<Rule>> group(List<Rule> beans){
        return beans.stream().collect(Collectors.groupingBy(Rule::getCategory));
    }
    
    
    public static void main(String[]args){
        Rule r1 = new Rule();
        r1.setCategory("test");
        Rule r2 = new Rule();
        r2.setCategory("truc");
        Rule r3 = new Rule();
        r3.setCategory("test");
        
        List<Rule> rules = Arrays.asList(r1,r2,r3);
        
        Map<String,List<Rule>> map = group(rules);
        
        for(Map.Entry<String,List<Rule>> e: map.entrySet()){
            System.out.printf("%s %s", e.getKey(),e.getValue());
        }
        
    }
    
}
