package info.kgeorgiy.ja.Maksonov.student;

import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;
import info.kgeorgiy.java.advanced.student.GroupName;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentQuery {

    private static final Comparator<Student> ID_COMPARATOR = Student::compareTo;
    private static final Comparator<Student> NAME_COMPARATOR = Comparator.comparing(Student::getLastName, Comparator.reverseOrder()).
            thenComparing(Student::getFirstName, Comparator.reverseOrder()).
            thenComparing(ID_COMPARATOR);

    //==================================================================================//

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return buildCollection(students.stream(), Student::getFirstName, Collectors.toList());
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return buildCollection(students.stream(), Student::getLastName, Collectors.toList());
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return buildCollection(students.stream(), Student::getGroup, Collectors.toList());
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return buildCollection(students.stream(), this::buildFullName, Collectors.toList());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return buildCollection(students.stream(), Student::getFirstName, Collectors.toCollection(HashSet::new));
    }

    //==========================================================//
    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(ID_COMPARATOR).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortCollection(students.stream(), ID_COMPARATOR, Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortCollection(students.stream(), NAME_COMPARATOR, Collectors.toList());
    }

    //==========================================================//

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String firstName) {
        return filterCollection(students.stream(), x -> x.getFirstName().equals(firstName), Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String lastName) {
        return filterCollection(students.stream(), x -> x.getLastName().equals(lastName), Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return filterCollection(students.stream(), x -> x.getGroup().equals(group), Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return filterCollection(students.stream(), x -> x.getGroup().equals(group),
                Collectors.toMap(Student::getLastName, Student::getFirstName, BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public List<Map.Entry<String, String>> findStudentNamesByGroupList(List<Student> students, GroupName group) {
        return StudentQuery.super.findStudentNamesByGroupList(students, group);
    }

    //==========================================================//

    private String buildFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private <S, T extends Collection<S> > T buildCollection(Stream<Student> studentStream, Function<? super Student, ? extends S> function, Collector<? super S, ?, T> collector) {
        return studentStream.map(function).collect(collector);
    }

    private <T> T sortCollection(Stream<Student> studentStream, Comparator<Student> comparator,  Collector<? super Student, ?, T> collector) {
        return studentStream.sorted(comparator).collect(collector);
    }

    private <T> T filterCollection(Stream<Student> studentStream, Predicate<? super Student> predicate, Collector<? super Student, ?, T> collector) {
        return studentStream.filter(predicate).sorted(NAME_COMPARATOR).collect(collector);
    }

}

