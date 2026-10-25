package com.club.config;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
@Component
public class SchemaUpgrade implements ApplicationRunner {
 private final DataSource source;
 public SchemaUpgrade(DataSource source){this.source=source;}
 public void run(ApplicationArguments args)throws Exception {
  try(var c=source.getConnection()) {
   try(var rs=c.getMetaData().getTables(c.getCatalog(),null,"user",null)){
    if(!rs.next())throw new IllegalStateException("数据库缺少原始表，请先导入新安装脚本；升级不会自动清空或重建数据库");
   }
   ScriptUtils.executeSqlScript(c,new ClassPathResource("db/upgrade-20261024.sql"));
   ScriptUtils.executeSqlScript(c,new ClassPathResource("db/upgrade-boot.sql"));
   try(var s=c.createStatement()){s.executeUpdate("INSERT IGNORE INTO schema_history(version) VALUES('20261024-boot-v1')");}
  }
 }
}
