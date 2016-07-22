/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.bugkillers.section;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static org.bugkillers.section.SectionUtil.decreaseByDays;
import static org.bugkillers.section.SectionUtil.isDate;
import static org.bugkillers.section.SectionUtil.isDateContinuous;

/**
 * 区间运算工具类
 * Created by liuxinyu on 15/11/19.
 */
public final class Section<T> implements Comparable<Section<T>> {

    /**
     * 区间左端点
     */
    private int startDate;

    /**
     * 区间右端点
     */
    private int endDate;

    /**
     * 比较器
     */
    private Comparator<T> comparator;

    /**
     * 区间数据
     */
    private T data;

    /**
     * 相等
     */
    public static final int EQUAL = 0;

    /**
     * 大于
     */
    public static final int MORE_THAN = 1;

    /**
     * 小于
     */
    public static final int LESS_THAN = -1;

    /**
     * 相邻的日期
     */
    private static final int ADJACENT_DAYS = 2;

    /**
     * 获取sectionList中的数据
     *
     * @param sectionList
     * @param <T>
     * @return
     */
    public static <T> List<T> getDataList(List<Section<T>> sectionList) {
        checkArgument(CollectionUtils.isNotEmpty(sectionList), "sectionList can't be empty");
        List<T> dataList = Lists.transform(sectionList, new Function<Section<T>, T>() {
            @Override
            public T apply(Section<T> tSection) {
                return tSection.data;
            }
        });
        return dataList;
    }

    public static <T> List<Section<T>> merge(List<Section<T>> source, List<Section<T>> target) {
        return merge(source, target, true);
    }

    /**
     * merge API (通用)
     * 合并两个Section集合
     *
     * @param source
     * @param target
     * @param <T>
     * @return
     */
    public static <T> List<Section<T>> merge(List<Section<T>> source, List<Section<T>> target, boolean mergeSelf) {

        if (mergeSelf && CollectionUtils.isNotEmpty(source)) {
            source = mergeSelf(source);
        }

        if (mergeSelf && CollectionUtils.isNotEmpty(target)) {
            target = mergeSelf(target);
        }

        //原始source为空直接返回target
        if (CollectionUtils.isEmpty(source)) {
            return target;
        }

        checkArgument(CollectionUtils.isNotEmpty(target), "target can't be empty");

        for (Section<T> targetSection : target) {
            source = merge(source, targetSection, false);
        }

        List<Section<T>> targetSectionList = Lists.newArrayList(source);

        return targetSectionList;
    }

    /**
     * merge API
     * <p/>
     * 合并两个Section（核心）
     *
     * @param source
     * @param target
     * @param <T>
     * @return
     */
    public static <T> List<Section<T>> merge(Section<T> source, Section<T> target) {

        //source为空返回target
        if (source == null) {
            return target == null ? null : Lists.newArrayList(target);
        }

        checkArgument(null != target, "target can't be null");

        //判断区间是否相连
        if (source.isConnected(target)) {
            //连接区间
            return Lists.newArrayList(connect(source, target));
        }

        final int minSourceDate = source.startDate;
        final int maxSourceDate = source.endDate;
        final int minTargetDate = target.startDate;
        final int maxTargetDate = target.endDate;

        //=====================区间计算逻辑====================================================

        /*
                source:   |_________|

                target:   |         |
                          |_________|

         */
        if (!source.isIntersected(target)) {
            /*
            case 0:没有交集
                                            |         |
                                |_________| |_________|
            */
            return Lists.newArrayList(source, target);
        }

        if (minSourceDate == minTargetDate && maxSourceDate == maxTargetDate) {
            /*
            case 99:无需区间计算

                        |_________|
                        |_________|

             */
            return Lists.newArrayList(target);
        }

        List<Section<T>> targetSectionList = Lists.newArrayList();

        if (minSourceDate <= minTargetDate) {
            if (maxSourceDate < maxTargetDate) {
                /*
                case 1: target和source相交

                        |________|______|    |
                                 |___________|

                */

                Section<T> section1 = build(minSourceDate, getBoundaryPoint(minSourceDate, minTargetDate, 1),
                        source.data, source.comparator);

                Section<T> section2 = build(minTargetDate, maxTargetDate, target.data, target.comparator);

                targetSectionList = Lists.newArrayList(section1, section2);

                if (minSourceDate == minTargetDate) {
                    //特殊情況处理

                     /*
                         case 1.1: target和source相交

                                 |___________|    |
                                 |________________|

                     */

                    //移除掉边界
                    targetSectionList.remove(section1);

                }

            } else {
                /*
                case 2:target在source之中

                              |        |
                              |________|
                        |_____________________|


                 */

                Section<T> section1 = build(minSourceDate, getBoundaryPoint(minSourceDate, minTargetDate, 1),
                        source.data, source.comparator);
                Section<T> section2 = build(minTargetDate, maxTargetDate, target.data, target.comparator);
                Section<T> section3 = build(getBoundaryPoint(maxSourceDate, maxTargetDate, -1), maxSourceDate,
                        source.data, source.comparator);

                targetSectionList = Lists.newArrayList(section1, section2, section3);

                if (minSourceDate == minTargetDate) {
                    //特殊情況处理

                     /*
                         case 2.1: target和source相交

                                 |___________|    |
                                 |________________|

                     */

                    //移除掉边界
                    targetSectionList.remove(section1);

                }

                if (maxSourceDate == maxTargetDate) {
                    //特殊情況处理

                     /*
                         case 2.2: target和source相交

                                 |    |___________|
                                 |________________|

                     */

                    //移除掉边界
                    targetSectionList.remove(section3);

                }

            }
        } else {
            if (maxSourceDate >= maxTargetDate) {
                /*
                case 3:target和source相交

                        |     |_____|_____|
                        |___________|

                 */

                Section<T> section1 = build(getBoundaryPoint(maxSourceDate, maxTargetDate, -1), maxSourceDate,
                        source.data, source.comparator);
                Section<T> section2 = build(minTargetDate, maxTargetDate, target.data, target.comparator);

                targetSectionList = Lists.newArrayList(section1, section2);

                if (maxSourceDate == maxTargetDate) {
                    //特殊情況处理

                     /*
                         case 3.1: target和source相交

                                 |    |___________|
                                 |________________|

                     */
                    targetSectionList.remove(section1);
                }

            } else {
                /*
                case 4:  target完全覆盖了source

                        |   |_________|  |
                        |________________|

                 */
                targetSectionList = Lists
                        .newArrayList(build(minTargetDate, maxTargetDate, target.data, target.comparator));
            }
        }

        return targetSectionList;
    }

    /**
     * merge API  核心
     * <p/>
     * 合并一个Section List 和 一个Section
     *
     * @param source
     * @param target
     * @param <T>
     * @return
     */
    public static <T> List<Section<T>> merge(List<Section<T>> source, Section<T> target, boolean mergeSelf) {

        if (mergeSelf && CollectionUtils.isNotEmpty(source)) {
            source = mergeSelf(source);
        }

        if (CollectionUtils.isEmpty(source)) {
            return target == null ? null : Lists.newArrayList(target);
        }

        checkArgument(null != target, "target can't be empty");

        List<Section<T>> targetSectionList = Lists.newArrayList(source);

        //每次merge之后新产生的Section放到此set
        List<Section<T>> tempList = Lists.newArrayList();

        tempList.add(target);

        while (!tempList.isEmpty()) {

            Iterator<Section<T>> iterator = source.iterator();

            Section<T> needMergeSection = null;

            while (iterator.hasNext()) {
                Section<T> sourceSection = iterator.next();
                if (sourceSection.isConnected(target) || sourceSection.isIntersected(target)) {
                    needMergeSection = sourceSection;
                    break;
                }
            }

            if (needMergeSection != null) {
                //将merge前的数据清除
                tempList.remove(target);
                targetSectionList.remove(needMergeSection);

                tempList.addAll(merge(needMergeSection, target));

                //递归调用
                return merge(targetSectionList, tempList, false);
            } else {
                targetSectionList.add(target);
                tempList.remove(target);
            }

        }

        return targetSectionList;
    }

    /**
     * merge API
     * <p/>
     * 合并一个Section对象和一个Section List
     *
     * @param source
     * @param target
     * @param <T>
     * @return
     */
    public static <T> List<Section<T>> merge(Section<T> source, List<Section<T>> target, boolean mergeSelf) {

        if (mergeSelf && CollectionUtils.isNotEmpty(target)) {
            target = mergeSelf(target);
        }

        if (source == null) {
            return target;
        }

        checkArgument(CollectionUtils.isNotEmpty(target), " target can't be empty");

        return merge(Lists.newArrayList(source), target, false);
    }

    /**
     * hit API
     * <p/>
     * 根据start命中一条则返回，start没有命中则认为不存在（不再排序） endDate暂时预留
     *
     * @param source
     * @param startDate
     * @param endDate
     * @param <T>
     * @return
     */
    public static <T> T hit(List<Section<T>> source, int startDate, int endDate) {

        checkArgument(CollectionUtils.isNotEmpty(source), "source can't be empty");

        for (Section<T> section : source) {
            if (section.startDate <= startDate && startDate <= section.endDate) {
                return section.data;
            }
        }

        return null;
    }

    /**
     * hit API  预留
     * <p/>
     * 根据start和end命中一条data(可自定义比较器)
     * <p/>
     * 暂时不提供排序逻辑
     *
     * @param source
     * @param comparator
     * @param startDate
     * @param endDate
     * @param <T>
     * @return
     */
    @Deprecated
    private static <T> T hit(List<Section<T>> source, Comparator<T> comparator, int startDate, int endDate) {

        checkArgument(CollectionUtils.isNotEmpty(source), "source can't be empty");

        checkArgument(isDate(startDate) && isDate(endDate) && startDate <= endDate,
                "start and end is illegal,that must comply with the format:'yyyyMMdd',now is startDate:%s endDate:%s.",
                startDate, endDate);

        List<Section<T>> hitSectionList = Lists.newArrayList();

        //命中判断
        for (Section<T> section : source) {
            if ((startDate >= section.startDate && startDate <= section.endDate) || (endDate >= section.startDate
                    && endDate <= section.endDate)) {
                //开始时间或结束时间命中
                hitSectionList.add(section);
            }
        }

        if (CollectionUtils.isEmpty(hitSectionList)) {
            return null;
        }

        List<T> originLists = getDataList(hitSectionList);

        TreeSet<T> sortSet = sort(originLists, comparator);

        return sortSet.first();
    }

    /**
     * 对data进行排序
     *
     * @param source
     * @param comparator
     * @param <T>
     * @return
     */
    private static <T> TreeSet<T> sort(List<T> source, Comparator<T> comparator) {
        checkArgument(CollectionUtils.isNotEmpty(source) && null != comparator, "source and comparator can't be empty");
        TreeSet<T> sortSet = new TreeSet<>(comparator);
        sortSet.addAll(source);
        return sortSet;
    }

    /**
     * 合并两个相邻的Section
     *
     * @param one
     * @param other
     * @param <T>
     * @return
     */
    public static <T> Section<T> connect(Section<T> one, Section<T> other) {

        checkArgument(one != null && other != null, "one and other can't be empty");
        checkArgument(one.isConnected(other), "two section must be isConnected");

        return build(Math.min(one.startDate, other.startDate), Math.max(other.endDate, one.endDate), one.data,
                one.comparator);
    }

    /**
     * 判断多个区间是否存在区间重复
     * <p/>
     * [1,4] [5,9]  no repeat
     * [1,4] [4,9]  repeat
     * [1,4] [3,9]  repeat
     *
     * @param sections
     * @return true 存在重复  false 不重复
     */
    public static boolean hasRepeat(List<Section> sections) {
        checkArgument(CollectionUtils.isNotEmpty(sections), "sections can't be empty");

        Section[] sectionArray = sections.toArray(new Section[0]);

        int length = sectionArray.length;

        //对起始时间排序
        quickSort(sectionArray, 0, length - 1);

        for (int i = 1; i < length; i++) {
            //出现跨区间
            if (sectionArray[i - 1].startDate >= sectionArray[i].startDate
                    || sectionArray[i - 1].endDate >= sectionArray[i].startDate) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对象构造器
     *
     * @param startDate
     * @param endDate
     * @param data
     * @param comparator
     * @param <T>
     * @return
     */
    public static <T> Section<T> build(int startDate, int endDate, T data, Comparator comparator) {

        checkArgument(data != null && comparator != null, "data and comparator can't be null");

        checkArgument(isDate(startDate) && isDate(endDate) && startDate <= endDate,
                "start and end is illegal,that must comply with the format:'yyyyMMdd',now is startDate:%s endDate:%s.",
                startDate, endDate);

        return new Section<>(startDate, endDate, data, comparator);
    }

    /**
     * 自己跟自己merge
     *
     * @param sectionList
     * @param <T>
     * @return
     */
    private static <T> List<Section<T>> mergeSelf(List<Section<T>> sectionList) {

        checkArgument(CollectionUtils.isNotEmpty(sectionList), "sectionList can't be empty");
        if (sectionList.size() == 1) {
            return sectionList;
        }

        List<Section<T>> tmpList = new ArrayList<>();
        tmpList.add(sectionList.get(0));
        for (int i = 1; i < sectionList.size(); i++) {
            tmpList = merge(tmpList, sectionList.get(i), false);
        }

        return tmpList;
    }

    /**
     * 快速排序的一次划分
     *
     * @param a
     * @param b
     * @param e
     * @return
     */
    private static int partition(Section a[], int b, int e) {
        Section temp = a[b];
        while (b < e) {
            while (b < e && a[e].startDate >= temp.startDate)
                e--;
            if (b < e)
                a[b] = a[e];
            while (b < e && a[b].startDate <= temp.startDate)
                b++;
            if (b < e)
                a[e] = a[b];
        }
        a[b] = temp;
        return b;
    }

    /**
     * 快排
     *
     * @param a
     * @param b
     * @param e
     */
    private static void quickSort(Section a[], int b, int e) {
        if (b >= e)
            return;
        int i = partition(a, b, e);
        quickSort(a, b, i - 1);
        quickSort(a, i + 1, e);
    }

    /**
     * 获取边界点
     *
     * @param startDate
     * @param endDate
     * @param value
     * @return
     */
    private static int getBoundaryPoint(int startDate, int endDate, int value) {
        endDate = decreaseByDays(endDate, value);
        return value < 0 ^ endDate <= startDate ? startDate : endDate;
    }

    /**
     * 判断当前section是否和另一section相连
     * 连接的的前提是两段的数据一致,同时两个区间段可以合并成一个区间段
     *
     * @param other
     * @return
     */
    public boolean isConnected(Section<T> other) {

        checkArgument(other != null, "other can't be empty");

        //判断数据是否相等
        if (this.comparator.compare(this.data, other.data) != EQUAL) {
            return false;
        }

        //判断区间是否相交或连续
        return this.isIntersected(other) || this.isContinuous(other);

    }

    /**
     * 判断和另一个区间是否相交(区间时间上)
     *
     * @param other
     * @return
     */
    public boolean isIntersected(Section<T> other) {
        checkArgument(other != null, "other can't be empty");
        return !(this.endDate < other.startDate || this.startDate > other.endDate);
    }

    /**
     * 判断和另一个区间是否是连续的(区间时间上--自然日的跨度)
     *
     * @param other
     * @return
     */
    public boolean isContinuous(Section<T> other) {
        checkArgument(other != null, "other can't be empty");
        if (this.endDate < other.startDate) {
            return isDateContinuous(this.endDate, other.startDate);
        }
        if (other.endDate < this.startDate) {
            return isDateContinuous(other.endDate, this.startDate);
        }

        return false;
    }

    /**
     * 构造器
     */
    private Section() {

    }

    /**
     * 私有构造器
     *
     * @param startDate
     * @param endDate
     * @param data
     * @param comparator
     */
    private Section(int startDate, int endDate, T data, Comparator<T> comparator) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.comparator = comparator;
        this.data = data;
    }

    /**
     * Section去重时使用
     *
     * @param o
     * @return
     */
    @Override
    public int compareTo(Section<T> o) {

        if (o == null) {
            return this == null ? EQUAL : LESS_THAN;
        }
        Section<T> that = o;

        if (this.startDate != that.startDate) {
            return this.startDate - that.startDate;
        }

        if (this.endDate != that.endDate) {
            return this.endDate - that.endDate;
        }

        return this.comparator.compare(this.data, that.data);
    }

    public int getLowerPoint() {
        return this.startDate;
    }

    public int getUpperPoint() {
        return this.endDate;
    }

    public T getData() {
        return data;
    }
}
