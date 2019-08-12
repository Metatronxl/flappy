# flappy





## 可能存在的问题

#### 0x01 Maven 多模块项目生成
```
mvn archetype:generate -DgroupId=com.codebelief.app -DartifactId=module-dao -DinteractiveMode=false

```


#### 0x01 Maven管理多模块项目编译

在根目录下的`pom.xml`下添加
```
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <classifier>exec</classifier>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
其他子模块不需要更改，在执行`mvn clean package`后会生成`xxx.jar`和`xxx-exec.jar`两个jar包，后者为我们可以用来直接使用的jar包
但这种情况会导致打包整个工程的所有模块，非常的臃肿和笨重，并且每个子模块还必须为SpringBoot的项目，不然会报错，更加推荐使用下一种打包方案


#####  !!!只打包单个文件的

在多module的maven项目中，如果每次打包整个工程显得有些冗余和笨重。
在根目录下执行 `mvn clean package install -pl xxx -am`（xxx为模块的名称）


# TODO

- 配置信息使用web修改（配置信息暂时放置在 /.lanproxy/config.json）

- 去掉配置信息中的server IP，改由自动获取

- 配置热更新时的处理逻辑