package info.kgeorgiy.ja.osipov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {

    private final Comparator<Student> studentByNameComparator = Comparator
            .comparing(Student::getLastName).reversed()
            .thenComparing(Comparator.comparing(Student::getFirstName).reversed())
            .thenComparingInt(Student::getId);

    private final Comparator<Map.Entry<GroupName, Long>> groupBySizeThenNameComparator =
            Comparator
                    .comparingLong(Map.Entry<GroupName, Long>::getValue)
                    .thenComparing(Map.Entry::getKey);

    // :NOTE: 2 одинаковых компаратора
    private final Comparator<Map.Entry<GroupName, Set<String>>> groupByDistinctNamesThenNameReverseComparator =
            Comparator
                    .<Map.Entry<GroupName, Set<String>>>comparingLong(entry -> entry.getValue().size())
                    .thenComparing(Map.Entry::getKey, Comparator.reverseOrder());

    private final Comparator<Map.Entry<String, Set<GroupName>>> nameByDistinctGroupThenNameReverseComparator =
            Comparator
                    .<Map.Entry<String, Set<GroupName>>>comparingLong(entry -> entry.getValue().size())
                    .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder()));

    private final Comparator<Map.Entry<String, Integer>> maxNameComparator =
            Map.Entry.comparingByValue();

    private List<Group> getListGroupBy(final Collection<Student> collection,
                                       final UnaryOperator<List<Student>> operator) {
        return collection
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup))
                .entrySet()
                .stream()
                .map(entry -> new Group(entry.getKey(), operator.apply(entry.getValue())))
                .sorted(Comparator.comparing(Group::getName))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> collection) {
        return getListGroupBy(collection, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> collection) {
        return getListGroupBy(collection, this::sortStudentsById);
    }

    private <R, T> T getMaxBy(final Collection<Student> collection,
                              final Collector<Student, ?, Map<T, R>> collector,
                              final Comparator<Map.Entry<T, R>> comparator,
                              final T empty) {
        return collection
                .stream()
                .collect(collector)
                .entrySet()
                .stream()
                .max(comparator)
                .map(Map.Entry::getKey)
                .orElse(empty);

    }

    private <R> GroupName getLargestGroupBy(final Collection<Student> collection,
                                            final Collector<Student, ?, R> collector,
                                            final Comparator<Map.Entry<GroupName, R>> comparator) {
        return getMaxBy(collection, Collectors.groupingBy(Student::getGroup, collector), comparator, null);
    }

    @Override
    public String getMostPopularName(final Collection<Student> collection) {
        return getMaxBy(collection,
                Collectors.groupingBy(Student::getFirstName, Collectors.mapping(Student::getGroup, Collectors.toSet())),
                nameByDistinctGroupThenNameReverseComparator,
                "");
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> list) {

        return getMaxBy(list, Collectors.toMap(
                Student::getFirstName,
                Student::getId,
                Math::max), maxNameComparator, "");
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> collection) {
        return getLargestGroupBy(
                collection,
                Collectors.counting(),
                groupBySizeThenNameComparator);
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> collection) {
        return getLargestGroupBy(
                collection,
                Collectors.mapping(Student::getFirstName, Collectors.toSet()),
                groupByDistinctNamesThenNameReverseComparator);
    }

    private <R, C extends Collection<R>> C getCollectionStudentsField(final Collection<Student> list,
                                                           final Function<Student, R> function,
                                                           final Supplier<C> collector) {
        return list.stream().map(function).collect(Collectors.toCollection(collector));
    }

    @Override
    public List<String> getFirstNames(final List<Student> list) {
        return getCollectionStudentsField(list, Student::getFirstName, ArrayList::new);
    }

    @Override
    public List<String> getLastNames(final List<Student> list) {
        return getCollectionStudentsField(list, Student::getLastName, ArrayList::new);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> list) {
        return getCollectionStudentsField(list, Student::getGroup, ArrayList::new);
    }

    private final Function<Student, String> getFullName =
            student -> String.join(" ", student.getFirstName(), student.getLastName());

    @Override
    public List<String> getFullNames(final List<Student> list) {
        return getCollectionStudentsField(
                list,
                getFullName,
                ArrayList::new);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> list) {
        return getCollectionStudentsField(list, Student::getFirstName, TreeSet::new);
    }

    private List<Student> sortStudentsBy(final Collection<Student> collection, final Comparator<Student> comparator) {
        return collection.stream().sorted(comparator).toList();
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> collection) {
        return sortStudentsBy(collection, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> collection) {
        return sortStudentsBy(collection, studentByNameComparator);
    }
    private List<Student> findStudentsByField(final Collection<Student> collection,
                                              final Predicate<Student> predicate) {
        return sortStudentsByName(collection.stream().filter(predicate).toList());
    }

    private <T> Predicate<Student> fieldEquals(Function<Student, T> f, T t) {
        return student -> f.apply(student).equals(t);
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> collection, final String s) {
        return findStudentsByField(collection, fieldEquals(Student::getFirstName, s));
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> collection, final String s) {
        return findStudentsByField(collection, fieldEquals(Student::getLastName, s));
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> collection, final GroupName groupName) {
        return findStudentsByField(collection, fieldEquals(Student::getGroup, groupName));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> collection,
                                                       final GroupName groupName) {
        return collection
                .stream()
                .filter(fieldEquals(Student::getGroup, groupName))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(Comparator.naturalOrder())));
    }

    private static Map<Integer, Student> mapIdStudent(final Collection<Student> students) {
        return students
                .stream()
                .collect(Collectors.toMap(Student::getId, student -> student));
    }

    private static <R> List<R> idsToValues(
            final Map<Integer, Student> studentMap,
            final int[] ids,
            final Function<Student, R> function) {
        return Arrays
                .stream(ids)
                .mapToObj(studentMap::get)
                .map(function)
                .toList();
    }

    private <R> List<R> getWithInts(final Collection<Student> students,
                                    final int[] ids,
                                    final Function<Student, R> function) {
        return idsToValues(mapIdStudent(students), ids, function);
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> collection, final int[] ids) {
        return getWithInts(collection, ids, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> collection, final int[] ids) {
        return getWithInts(collection, ids, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> collection, final int[] ids) {
        return getWithInts(collection, ids, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> collection, final int[] ids) {
        return getWithInts(collection, ids, getFullName);
    }
}
