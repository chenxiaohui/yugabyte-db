// Copyright (c) YugaByte, Inc.
package org.yb.cql;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.*;
import java.util.TreeSet;

public class TestDescendingOrder extends TestBase {
  private void createTable(String type1, String type2, String orderStmt) throws Exception {
    String createStmt = String.format("CREATE TABLE test_create " +
                                      "(h1 int, r1 %s, r2 %s, v1 int, v2 varchar, " +
                                      "primary key((h1), r1, r2)) %s;", type1, type2, orderStmt);
    LOG.info("createStmt: " + createStmt);
    session.execute(createStmt);
  }

  private void dropTable() throws Exception {
    String drop_stmt = "DROP TABLE test_create;";
    session.execute(drop_stmt);
  }

  private ResultSet createInsertAndSelectDesc(String dataType, List<String> values)
      throws Exception {

    createTable(dataType, "varchar", "WITH CLUSTERING ORDER BY(r1 DESC)");

    for (String s : values) {
      String insertStmt = String.format("INSERT INTO test_create (h1, r1, r2, v1, v2) " +
                                        "VALUES (1, %s, 'b', 1, 'c');", s);
      session.execute(insertStmt);
    }

    String selectStmt = "SELECT h1, r1, r2, v1, v2 FROM test_create WHERE h1 = 1";
    return session.execute(selectStmt);
  }

  private void intDesc(String type, long lower_bound, long upper_bound) throws Exception {
    LOG.info("TEST CQL " + type.toUpperCase() + " DESCENDING ORDER - START");
    // Create a unique list of random numbers.
    long[] values = new Random().longs(100, lower_bound, upper_bound).distinct().toArray();
    Arrays.sort(values);

    // Create a list of strings representing the integers in values.
    List<String> stringValues =
      Arrays.stream(values).mapToObj(value -> Long.toString(value)).collect(Collectors.toList());

    ResultSet rs = createInsertAndSelectDesc(type, stringValues);
    assertEquals(rs.getAvailableWithoutFetching(), values.length);

    // Rows should come sorted by column r1 in descending order.
    for (int i = values.length - 1; i >= 0; i--) {
      Row row = rs.one();
      assertEquals(1, row.getInt("h1"));
      long r1;
      switch(type) {
        case "bigint":
          r1 = row.getLong("r1"); break;
        case "int":
          r1 = row.getInt("r1"); break;
        case "smallint":
          r1 = row.getShort("r1"); break;
        case "tinyint":
          r1 = row.getByte("r1"); break;
        default:
          throw new Exception("Invalid data type " + type);
      }
      assertEquals(values[i], r1);
      assertEquals("b", row.getString("r2"));

      assertEquals(1, row.getInt("v1"));
      assertEquals("c", row.getString("v2"));
    }

    dropTable();
    LOG.info("TEST CQL " + type.toUpperCase() + " DESCENDING ORDER - END");
  }

  @Test
  public void testInt8Desc() throws Exception {
    intDesc("tinyint", -128, 127);
  }

  @Test
  public void testInt16Desc() throws Exception {
    intDesc("smallint", -32768, 32767);
  }

  @Test
  public void testInt32Desc() throws Exception {
    intDesc("int", Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  @Test
  public void testInt64Desc() throws Exception {
    intDesc("bigint", Long.MIN_VALUE, Long.MAX_VALUE);
  }

  @Test
  public void testStringDesc() throws Exception {
    LOG.info("TEST CQL STRING DESCENDING ORDER - START");
    String characters =
        "ABCDEFGHIJKLMNOQRSTUVXYZabcdefghijklmnoqrstuvxyz0123456789-+*/?!@#$%^&(),.:; ";

    Random random = new Random();
    TreeSet<String> values = new TreeSet<String>();

    // Create a list of 20 random strings by using specific characters.
    for (int i = 0; i < 20; i++) {
      String s = "'";
      // Create a string with up to 100 characters.
      int length = random.nextInt(100) + 1;

      for (int j = 0; j < length; j++) {
        s += characters.charAt(random.nextInt(characters.length() - 1));
      }
      s += "'";
      values.add(s);
    }

    ResultSet rs = createInsertAndSelectDesc("varchar", new ArrayList<String>(values));
    assertEquals(rs.getAvailableWithoutFetching(), values.size());

    // Rows should come sorted by column r1 in descending order.
    for (Iterator<String> iter = values.descendingIterator(); iter.hasNext();) {
      Row row = rs.one();
      assertEquals(1, row.getInt("h1"));
      assertEquals(iter.next().replace("'",""), row.getString("r1"));
      assertEquals("b", row.getString("r2"));
      assertEquals(1, row.getInt("v1"));
      assertEquals("c", row.getString("v2"));
    }

    dropTable();
    LOG.info("TEST CQL STRING DESCENDING ORDER - END");
  }

  @Test
  public void testTimestampDesc() throws Exception {
    LOG.info("TEST CQL TIMESTAMP DESCENDING ORDER - START");
    final long currentTime = System.currentTimeMillis();
    final long[] values = new Random().longs(100, 0, 2 * currentTime).distinct().toArray();
    Arrays.sort(values);

    final List<String> stringValues =  Arrays.stream(values)
                                             .mapToObj(value -> Long.toString(value))
                                             .collect(Collectors.toList());

    ResultSet rs = createInsertAndSelectDesc("timestamp", stringValues);
    assertEquals(rs.getAvailableWithoutFetching(), values.length);

    // Rows should come sorted by column r1 in descending order.
    for (int i = values.length - 1; i >= 0; i--) {
      Row row = rs.one();
      assertEquals(1, row.getInt("h1"));
      Calendar calendar = new GregorianCalendar();
      calendar.setTimeInMillis(values[i]);
      assertEquals(calendar.getTime(), row.getTimestamp("r1"));
      assertEquals("b", row.getString("r2"));
      assertEquals(1, row.getInt("v1"));
      assertEquals("c", row.getString("v2"));
    }

    dropTable();
    LOG.info("TEST CQL TIMESTAMP DESCENDING ORDER - END");
  }

  @Test
  public void testInetDesc() throws Exception {
    List <String> values = new ArrayList<String>(
      Arrays.asList("'1.2.3.4'", "'180::2978:9018:b288:3f6c'", "'2.2.3.4'"));
    ResultSet rs = createInsertAndSelectDesc("inet", values);
    assertEquals(rs.getAvailableWithoutFetching(), values.size());

    // Rows should come sorted by column r1 in descending order.
    for (int i = values.size() - 1; i >= 0; i--) {
      Row row = rs.one();
      assertEquals(1, row.getInt("h1"));
      assertEquals(InetAddress.getByName(values.get(i).replace("\'", "")), row.getInet("r1"));
      assertEquals("b", row.getString("r2"));
      assertEquals(1, row.getInt("v1"));
      assertEquals("c", row.getString("v2"));
    }

    dropTable();
  }

  private void insertValues(int[] numbers, String[] strings) throws Exception {
    for (int number : numbers) {
      for (String s : strings) {
        String insertStmt = String.format("INSERT INTO test_create (h1, r1, r2, v1, v2) " +
                                          "VALUES (1, %s, '%s', 1, 'c');", number, s);
        session.execute(insertStmt);
      }
    }
  }

  private void selectAndVerifyValues(int[] numbers, String[] strings) throws Exception {
    String selectStmt = "SELECT h1, r1, r2, v1, v2 FROM test_create WHERE h1 = 1";
    ResultSet rs = session.execute(selectStmt);
    assertEquals(rs.getAvailableWithoutFetching(), numbers.length * strings.length);

    for (int i = 0; i < numbers.length; i++) {
      for (int j = 0; j < strings.length; j++) {
        Row row = rs.one();
        assertEquals(1, row.getInt("h1"));
        assertEquals(numbers[i], row.getInt("r1"));
        assertEquals(strings[j], row.getString("r2"));
        assertEquals(1, row.getInt("v1"));
        assertEquals("c", row.getString("v2"));
      }
    }
  }

  private void r1r2(int[] numbers, String[] strings,
                    int[] expected_numbers, String[] expected_strings,
                    String orderR1, String orderR2) throws Exception {
    final String testMsg = String.format("TEST CQL R1 %s, R2 %s - START ",
                                         orderR1.toUpperCase(), orderR2.toUpperCase());
    LOG.info(testMsg + " - START");

    createTable("int", "varchar",
                String.format("WITH CLUSTERING ORDER BY(r1 %s, r2 %s)", orderR1, orderR2));

    insertValues(numbers, strings);
    selectAndVerifyValues(expected_numbers, expected_strings);

    dropTable();
    LOG.info(testMsg + " - END");
  }

  @Test
  public void testR1DescR2Desc() throws Exception {
    r1r2(numbers, strings, reversed_numbers, reversed_strings, "DESC", "DESC");
  }

  @Test
  public void testR1DescR2Asc() throws Exception {
    r1r2(reversed_numbers, strings, reversed_numbers, strings, "DESC", "ASC");
  }

  @Test
  public void testR1AscR2Desc() throws Exception {
    r1r2(reversed_numbers, strings, numbers, reversed_strings, "ASC", "DESC");
  }

  @Test
  public void testR1AscR2Asc() throws Exception {
    r1r2(reversed_numbers, reversed_strings, numbers, strings, "ASC", "ASC");
  }

  private static int[] numbers = {3, 5, 9};
  private static String[] strings = {"ant", "bear", "cat", "dog", "eagle", "fox", "goat"};
  private static int[] reversed_numbers = {9, 5, 3};
  private static String[] reversed_strings =
      Arrays.stream(strings).sorted(Collections.reverseOrder()).toArray(String[]::new);
}