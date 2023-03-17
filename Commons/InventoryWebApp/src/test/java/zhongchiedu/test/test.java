package zhongchiedu.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class test {


   public static String fetchGroupKey(Student s){
       return s.getGrade()+"_"+s.getAddress()+"_"+s.getAge();
   }

    public static void main(String[] args) {

        Student student1 = new Student("701","张三","北京",16,78,90);
        Student student2 = new Student("700","李四","北京",17,78,90);
        Student student3 = new Student("703","王五","上海",16,78,90);
        Student student4 = new Student("701","赵六","北京",16,78,90);
        Student student5 = new Student("700","钱七","",18,78,90);
        Student student6 = new Student("701","老八","",17,78,90);
        List<Student> students= Arrays.asList(student1,student2,student3,student4,student5,student6);
        Map<String, List<Student>> newsts=students.stream().collect(Collectors.groupingBy(student -> fetchGroupKey(student)));
        for(String key: newsts.keySet()){
          Integer maths=newsts.get(key).stream().mapToInt(Student::getMathScores).sum();
          String[] ss=key.split("_");
            System.out.println(ss[1]);
        }
    }

}
