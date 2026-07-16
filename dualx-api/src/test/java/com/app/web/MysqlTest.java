package com.app.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 相关类生成
 */
public class MysqlTest extends DualxApplicationTests {

    /**
     * 数据库的配置 jdbc:mysql://127.0.0.1:3306/dualx_dev?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT%2B8
     */
    @Value("${spring.datasource.url}")
    private String sqlUrl;

    /**
     * 数据库用户名
     */
    @Value("${spring.datasource.username}")
    private String username;

    /**
     * 数据库密码
     */
    @Value("${spring.datasource.password}")
    private String password;

    /**
     * 代码生成
     */
    @Test
    public void codeGeneration() throws Exception{
        String[] table = {"banner"};
        String[] prefix = {"t_", "sys_"};
        Path rootDir = findRootDir();
        Path dbDir = findModuleDir(rootDir, "src/main/java/com/app/db");
        Path apiDir = findModuleDir(rootDir, "src/main/java/com/app/web");
        Map<OutputFile, String> pathInfo = new HashMap<>();
        pathInfo.put(OutputFile.entity, dbDir.resolve("src/main/java/com/app/db/entity").toString()); // 实体类生成目录
        pathInfo.put(OutputFile.service, apiDir.resolve("src/main/java/com/app/web/service").toString()); // 服务接口生成目录
        pathInfo.put(OutputFile.serviceImpl, apiDir.resolve("src/main/java/com/app/web/service/impl").toString()); // 服务实现类生成目录
        pathInfo.put(OutputFile.mapper, dbDir.resolve("src/main/java/com/app/db/mapper").toString()); // Mapper接口生成目录
        pathInfo.put(OutputFile.xml, dbDir.resolve("src/main/resources/mapper").toString()); // Mapper XML生成目录
        pathInfo.put(OutputFile.controller, apiDir.resolve("src/main/java/com/app/web/api").toString()); // 控制器生成目录

        FastAutoGenerator.create(sqlUrl, username, password)
            .globalConfig(builder -> {
                builder.author("Auto") // 设置作者
                    .enableSwagger(); // 开启 swagger 模式
                //.fileOverride() // 注释掉可以避免覆盖已有文件
            })
            .packageConfig(builder -> {
                builder.parent("com.app") // 设置父包名
                    .entity("db.entity") // 设置实体类包名
                    .service("web.service") // 设置服务类包名
                    .serviceImpl("web.service.impl") // 设置服务实现类包名
                    .mapper("db.mapper") // 设置Mapper接口包名
                    .controller("web.api") // 设置控制器包名
                    .pathInfo(pathInfo); // 设置不同类型类的生成路径
            })
            .strategyConfig(builder -> {
                builder.entityBuilder().enableLombok(); // entity中使用lombok
                builder.mapperBuilder().enableMapperAnnotation().build(); // mapper接口中默认添加mapper注解
                builder.controllerBuilder().enableHyphenStyle()  // 开启驼峰转连字符
                    .enableRestStyle();  // 开启生成@RestController 控制器
                builder.addInclude(table) // 设置需要生成的表名
                    .addTablePrefix(prefix); // 设置过滤表前缀
            })
            .injectionConfig(builder -> {
                builder.beforeOutputFile((tableInfo, objectMap) -> {
                    // 仅覆盖实体类
                    String entityFile = pathInfo.get(OutputFile.entity) + File.separator + tableInfo.getEntityName() + ".java";
                    File file = new File(entityFile);
                    if (file.exists()) {
                        file.delete(); // 删除已有的实体类文件
                    }
                });
            })
            .execute();
    }

    private Path findRootDir() {
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (dir != null) {
            if (dir.resolve("pom.xml").toFile().isFile() && isMultiModuleRoot(dir)) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Cannot find Maven multi-module project root");
    }

    private boolean isMultiModuleRoot(Path dir) {
        try {
            return Files.list(dir)
                .filter(Files::isDirectory)
                .map(path -> path.resolve("pom.xml"))
                .anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            return false;
        }
    }

    private Path findModuleDir(Path rootDir, String markerPath) throws IOException {
        List<Path> matches;
        try {
            matches = Files.list(rootDir)
                .filter(Files::isDirectory)
                .filter(path -> path.resolve("pom.xml").toFile().isFile())
                .filter(path -> path.resolve(markerPath).toFile().isDirectory())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IOException("Cannot scan modules under " + rootDir, e);
        }
        if (matches.size() != 1) {
            throw new IllegalStateException(String.format(
                "Expected one module containing %s, but found %s",
                markerPath,
                Arrays.toString(matches.toArray())
            ));
        }
        return matches.get(0);
    }

}
