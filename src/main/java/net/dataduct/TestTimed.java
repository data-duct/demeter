package net.dataduct;

import net.dataduct.annotation.Counter;
import net.dataduct.annotation.Monitor;
import net.dataduct.annotation.Time;
import net.dataduct.registry.DemeterMonitoring;

@Monitor
public class TestTimed {

  public static void main(String[] args) {
    System.out.println("hello test");
    try {
      DemeterMonitoring.init(8070);
      divya();
      divyaCount("divyacount");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Time
  private static void divya() {
    try {
      Thread.sleep(200);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("hello test2");
  }

  private static void divyaCount(@Counter(name = "divya counter") String test) {
    System.out.println("hello test3");
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
