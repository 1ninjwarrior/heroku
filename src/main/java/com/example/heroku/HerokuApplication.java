/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.heroku;
import org.springframework.ui.Model;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

@Controller
@SpringBootApplication
public class HerokuApplication {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HerokuApplication.class, args);
  }
  public static class InputString {
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
  @RequestMapping("/")
  String index() {
    return "index";
  }
  @RequestMapping("/dbinput")
  public String dbInputForm(Model model) {
      model.addAttribute("inputString", new InputString());
      return "dbinput";
  }
  @PostMapping("/dbinput")
public String dbInputSubmit(@ModelAttribute InputString inputString) {
    // Insert the user input string into the database
    try (Connection connection = dataSource.getConnection()) {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO table_timestamp_and_random_string VALUES (now(), ?)");
        stmt.setString(1, inputString.getValue());
        stmt.executeUpdate();
    } catch (Exception e) {
        // Handle exception
    }

    return "redirect:/db";
}

  @RequestMapping("/db")
  String db(Map<String, Object> model) {
      try (Connection connection = dataSource.getConnection()) {
          Statement stmt = connection.createStatement();
          ResultSet rs = stmt.executeQuery("SELECT tick, random_string FROM table_timestamp_and_random_string");

          ArrayList<String> output = new ArrayList<String>();
          while (rs.next()) {
              output.add("Read from DB: " + rs.getTimestamp("tick") + " " + rs.getString("random_string"));
          }

          model.put("records", output);
          return "db";
      } catch (Exception e) {
          model.put("message", e.getMessage());
          return "error";
      }
  }

  private String getRandomString() {
    int length = 10;
    String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    Random random = new Random();
    StringBuilder stringBuilder = new StringBuilder(length);

    for (int i = 0; i < length; i++) {
        stringBuilder.append(characters.charAt(random.nextInt(characters.length())));
    }

    return stringBuilder.toString();
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
