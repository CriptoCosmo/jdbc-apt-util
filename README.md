#	JDBC APT UTIL

This library provide annotation for dynamic implementation of [org.springframework.jdbc.core.RowMapper](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/RowMapper.html)

##  Start 

```shell
git clone https://github.com/CriptoCosmo/jdbc-apt-util.git
mvn install
```



## Usage

Add dependecy in your `pom.xml` 

```xml
<dependency>
    <groupId>it.marbola.apt</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>0.0.1</version>
    <scope>compile</scope>
</dependency>
```

Add plugin in your `pom.xml` 

```xml
<build>
    <plugins>
		...
        <plugin>
            <groupId>com.mysema.maven</groupId>
            <artifactId>apt-maven-plugin</artifactId>
            <version>1.0.5</version>
            <executions>
                <execution>
                    <goals>
                        <goal>process</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>src/main/java</outputDirectory>
                        <processor>
                            it.marbola.jdbc.annotation.RowMapperProcessor
                        </processor>
                    </configuration>
                </execution>
            </executions>
        </plugin>
		...
    </plugins>
</build>
```

An example of Model class :

Model class must have a **default constructor** and **setter for all fields**

>  NOTE : primitive not working you have to use class wraps

> ```Java
> Byte,Time,Timestamp,Short,Long,Float,Double,Boolean,String,BigDecimal,Object
> ```



```java

package com.example.demo.model;

import lombok.Data;

@Data //Setter and getter is required
public class CustomEntity {

	private Float id ;
	private String name ;
	private String lastName ;
	private Integer size ;
	private Object obj ;

}

```

Now defining the interface as follow

```java
@MapRow //Here is magic
public interface CustomRowMapper extends RowMapper<CustomEntity> {}
```



# Generated

 when build annotation processor generate the following code

```java
package com.example.demo.impl;

import com.example.demo.model.CustomEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class CustomRowMapperImpl implements RowMapper<CustomEntity> {
  public CustomEntity mapRow(ResultSet resultSet, int row) {
    CustomEntity model = new CustomEntity();
    try {
      model.setId(resultSet.getFloat("ID"));
      model.setName(resultSet.getString("NAME"));
      model.setLastName(resultSet.getString("LASTNAME"));
      model.setSize(resultSet.getInt("SIZE"));
      model.setObj(resultSet.getObject("OBJ"));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return model;
  }
}

```
