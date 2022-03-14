package com.gregtao;

import org.jsoup.Jsoup; //外部库：Jsoup -- https://jsoup.org/
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        DataManager.getTyphoonLists(DataPool.startYear, DataPool.endYear);
        DataManager.writeTyphoonData();
        DataManager.writeAverageData();
    }

}

class DataManager {
    public static FileWriter totalWriter;

    public static void writeTyphoonData() {
        try {
            totalWriter = new FileWriter(DataPool.dataSaveTo + ".total.html");
            totalWriter.write("<table border=\"2\">" + TyphoonPoint.tableHead);

            for (TyphoonSeason season : DataPool.typhoonSeasons) {
                season.print(totalWriter);
            }

            totalWriter.write("</table>");
            totalWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeAverageData() {
        try {
            totalWriter = new FileWriter(DataPool.dataSaveTo + ".average.html");

            DataPool.compilePrefixSumDataAmount();
            totalWriter.write("<h3>1949-2000 数据条数：<br>");
            totalWriter.write(String.valueOf(DataPool.getDataAmount(1949, 2000)));
            totalWriter.write("</h3>");
            totalWriter.write("<h3>2001-" + DataPool.endYear + " 数据条数：<br>");
            totalWriter.write(String.valueOf(DataPool.getDataAmount(2001, 2021)));
            totalWriter.write("</h3>");

            totalWriter.write("<table border=\"2\">");
            totalWriter.write(DataPool.getTableLine("年份", "热带气旋数量"));
            for (TyphoonSeason season : DataPool.typhoonSeasons) {
                totalWriter.write(DataPool.getTableLine(String.valueOf(season.year), String.valueOf(season.typhoons.size())));
            }

            totalWriter.write("</table>");
            totalWriter.write("<br><hr><br>");
            DataPool.pressureMoveSpeed.printAsTable(totalWriter);
            totalWriter.write("<br><hr><br>");
            DataPool.pressureRadius.printAsTable(totalWriter);

            totalWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getTyphoonLists(int start, int end) {
        for (int i = start; i <= end; i++) {
            DataPool.typhoonSeasons.add(new TyphoonSeason(i));
            Document doc;
            try {
                doc = Jsoup.parse(new URL(DataPool.tyListUri + i), 10000);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Elements list = doc.getElementsByTag("typhoonlistmodel");
            for (Element e : list) {
                Typhoon typhoon = new Typhoon(
                        DataPool.getIntegerByTagName(e, "tfid"),
                        DataPool.getValueByTagName(e, "name"),
                        DataPool.getValueByTagName(e, "enname")
                );
                typhoon.getPoints();
                DataPool.putTyphoon(i, typhoon);
            }
        }
    }

}

class DataPool {
    public static String tyListUri = "http://typhoon.zjwater.gov.cn/Api/TyphoonList/";
    public static String tyInfoUri = "http://typhoon.zjwater.gov.cn/Api/TyphoonInfo/";

    public static String dataSaveTo = "./data/";

    public static int startYear = 1949;
    public static int endYear = 2021;

    public static List<TyphoonSeason> typhoonSeasons = new ArrayList<>();
    public static int[] prefixSumAmount = new int[endYear - startYear + 1];

    public static AverageHelper pressureMoveSpeed = new AverageHelper("气压", "平均移速");
    public static AverageHelper pressureRadius = new AverageHelper("气压", "平均7级风圈半径");

    public static void compilePrefixSumDataAmount() {
        for (TyphoonSeason season : typhoonSeasons) {
            int p = season.year - startYear, sum = 0;
            for (Typhoon typhoon : season.typhoons) {
                sum += typhoon.points.size();
            }
            prefixSumAmount[p] = sum + (p > 0 ? prefixSumAmount[p - 1] : 0);
        }
    }

    public static int getDataAmount(int s, int e) {
        s -= startYear;
        e -= startYear;
        if (s < 0 || e > endYear - startYear + 1 || s > e) return -1;
        if (s == 0) return prefixSumAmount[e];
        return prefixSumAmount[e] - prefixSumAmount[s - 1];
    }

    public static void putTyphoon(int year, Typhoon typhoon) {
        typhoonSeasons.get(year - startYear).typhoons.add(typhoon);
    }

    public static double radius7Average(Element e) {
        String r = getValueByTagName(e, "radius7");
        if (r.isEmpty()) return 0;
        String[] args = r.split("\\|", 4);
        double r1 = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; ++i) {
            r1 = (r1 + Integer.parseInt(args[i])) / 2;
        }
        return r1;
    }

    public static String getValueByTagName(Element e, String tag) {
        String str = e.getElementsByTag(tag).text().trim();
        return str.isEmpty() ? "0" : str;
    }

    public static int getIntegerByTagName(Element e, String tag) {
        return Integer.parseInt(getValueByTagName(e, tag));
    }

    public static double getDoubleByTagName(Element e, String tag) {
        return Double.parseDouble(getValueByTagName(e, tag));
    }

    public static String getTableLine(String ...v) {
        StringBuilder str = new StringBuilder("<tr>");
        for (String s : v) str.append("<td>").append(s).append("</td>");
        return str + "</tr>";
    }
}

class AverageHelper {
    public Map<Double, Double> numbers = new HashMap<>();
    public String key, val;

    public AverageHelper(String key, String val) {
        this.key = key;
        this.val = val;
    }

    public void putNumber(Double key, Double val) {
        if (this.numbers.containsKey(key)) {
            this.numbers.put(key, (this.numbers.get(key) + val) / 2);
        } else {
            this.numbers.put(key, val);
        }
    }

    public void putNumber(int key, int val) {
        double key1 = Double.parseDouble(String.valueOf(key));
        double val1 = Double.parseDouble(String.valueOf(val));
        this.putNumber(key1, val1);
    }

    public void putNumber(int key, double val) {
        double key1 = Double.parseDouble(String.valueOf(key));
        this.putNumber(key1, val);
    }

    public void printAsTable(FileWriter writer) throws IOException {
        writer.write("<table border=\"2\">");
        writer.write(DataPool.getTableLine(this.key, this.val));
        for (Map.Entry<Double, Double> entry : this.numbers.entrySet()) {
            writer.write(DataPool.getTableLine(entry.getKey().toString(), entry.getValue().toString()));
        }
        writer.write("</table>");
    }
}

class TyphoonSeason {
    public int year;
    public List<Typhoon> typhoons = new ArrayList<>();
    public AverageHelper pressureMoveSpeed = new AverageHelper("气压", "平均移速（本年度）");
    public AverageHelper pressureRadius = new AverageHelper("气压", "平均7级风圈半径（本年度）");

    public TyphoonSeason(int year) {
        this.year = year;
    }

    public void compileAverage() {
        for (Typhoon typhoon : this.typhoons) {
            for (TyphoonPoint point : typhoon.points) {
                if (point.pressure > 800 && point.moveSpeed > 0) this.pressureMoveSpeed.putNumber(point.pressure, point.moveSpeed);
                if (point.radius7 > 0 && point.pressure > 800) this.pressureRadius.putNumber(point.pressure, point.radius7);
            }
        }
    }

    public void print(FileWriter writer) {
        try {
            System.out.println("Writing data for " + this.year);
            FileWriter tyWriter = new FileWriter(DataPool.dataSaveTo + "data_" + this.year + ".html");
            this.compileAverage();

            tyWriter.write("<h3>年份：" + this.year + "</h3><h4>热带气旋数量：" + this.typhoons.size() + "</h4>");

            this.pressureMoveSpeed.printAsTable(tyWriter);
            tyWriter.write("<br><hr><br>");
            this.pressureRadius.printAsTable(tyWriter);
            tyWriter.write("<br><hr><br>");

            for (Typhoon typhoon : this.typhoons) {
                typhoon.print(tyWriter);
                typhoon.printNoTitle(writer);
            }
            tyWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class Typhoon {
    public Integer id;
    public String name;
    public String enName;
    public List<TyphoonPoint> points = new ArrayList<>();

    public Typhoon(int id, String name, String en) {
        this.id = id;
        this.name = name;
        this.enName = en;
        System.out.println("Parsing " + this.id);
    }

    public void getPoints() {
        Document doc;
        try {
            doc = Jsoup.parse(new URL(DataPool.tyInfoUri + this.id), 10000);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        Elements listForecast = doc.getElementsByTag("forecastmodel");
        listForecast.remove();
        Elements list = doc.getElementsByTag("typhoonpointmodel");
        for (Element e : list) {
            TyphoonPoint point = new TyphoonPoint(
                    DataPool.radius7Average(e),
                    DataPool.getIntegerByTagName(e, "power"),
                    DataPool.getValueByTagName(e, "strong"),
                    DataPool.getIntegerByTagName(e, "speed"),
                    DataPool.getIntegerByTagName(e, "movespeed"),
                    DataPool.getIntegerByTagName(e, "pressure"),
                    DataPool.getDoubleByTagName(e, "lat"),
                    DataPool.getDoubleByTagName(e, "lng"),
                    DataPool.getValueByTagName(e, "time"),
                    this.id
            );
            this.points.add(point);
        }
    }

    public void print(FileWriter writer) {
        try {
            writer.write("<h3>" + this.id + " " + this.name + " (" + this.enName + ")</h3><table border=\"2\">");
            writer.write(TyphoonPoint.tableHead);
            for (TyphoonPoint point : this.points) {
                writer.write(point.print());
            }
            writer.write("</table>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printNoTitle(FileWriter writer) {
        try {
            for (TyphoonPoint point : this.points) {
                writer.write(point.print());
                if (this.id < 200100) return; //不将2001年之前的数据纳入平均值
                if (point.pressure > 800 && point.moveSpeed > 0) DataPool.pressureMoveSpeed.putNumber(point.pressure, point.moveSpeed);
                if (point.radius7 > 0 && point.pressure > 800) DataPool.pressureRadius.putNumber(point.pressure, point.radius7);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class TyphoonPoint {
    public double radius7; //7级风圈半径（平均值）
    public int power; //强度等级
    public String strong; //级别
    public int windSpeed; //风速
    public int moveSpeed; //移速
    public int pressure; //气压
    public double lat; //纬度
    public double lng; //经度
    public String time;
    public int ofId; //所属热带气旋id

    public static String tableHead = DataPool.getTableLine(
            "七级风圈半径（平均值）", "强度等级", "级别",
            "风速", "移速", "气压", "纬度", "经度", "时间", "所属热带气旋ID"
    );

    public TyphoonPoint(double radius7, int power, String strong, int ws, int ms, int pressure, double lat, double lng, String time, int ofId) {
        this.radius7 = radius7;
        this.power = power;
        this.strong = strong;
        this.windSpeed = ws;
        this.moveSpeed = ms;
        this.pressure = pressure;
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.ofId = ofId;
    }

    public String print() {
        return DataPool.getTableLine(
                String.valueOf(this.radius7), String.valueOf(this.power), this.strong,
                String.valueOf(this.windSpeed), String.valueOf(this.moveSpeed), String.valueOf(this.pressure),
                String.valueOf(this.lat), String.valueOf(this.lng), this.time, String.valueOf(this.ofId)
        );
    }
}
