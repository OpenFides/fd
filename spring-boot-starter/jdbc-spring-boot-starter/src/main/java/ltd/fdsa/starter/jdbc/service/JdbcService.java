package ltd.fdsa.starter.jdbc.service;

import com.google.common.base.Strings;
import io.swagger.models.*;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.*;
import lombok.Getter;
import lombok.var;
import ltd.fdsa.database.entity.BaseEntity;
import ltd.fdsa.database.service.BaseService;
import ltd.fdsa.database.sql.columns.Column;
import ltd.fdsa.database.sql.schema.Table;
import ltd.fdsa.starter.jdbc.mappers.RowDataMapper;
import ltd.fdsa.starter.jdbc.properties.JdbcApiProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
public class JdbcService extends BaseService<BaseEntity<Integer>, Integer> implements InitializingBean {

    final JdbcApiProperties properties;

    public JdbcService(JdbcApiProperties properties) {
        this.properties = properties;
    }

    public Object query(String sql, Object[] args) {
        var jdbcTemplate = new JdbcTemplate(this.writer);
        return jdbcTemplate.query(sql, args, new RowDataMapper());
    }

    public Object create(String sql, Map<String, ?> args) {
        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(this.writer);

        return jdbcTemplate.update(sql, args);
    }


    public int update(String sql, @Nullable Object... args) {

        var jdbcTemplate = new JdbcTemplate(this.writer);
        return jdbcTemplate.update(sql, args);
    }

    public Swagger createTablesController(String host, String basePath) {

        Swagger swagger = new Swagger();
        {
            Info info = new Info();
            info.title("rest-ful api for database");
            info.description("It is simple to open the rest-ful api for database");
            Contact contact = new Contact();
            contact.email("zhumingwu@zhumingwu.cn");
            contact.name("zhumingwu");
            contact.url("http://blog.zhumingwu.cn");
            info.contact(contact);
            swagger.info(info);
        }
        swagger.host(host);
        swagger.basePath(basePath);

        Response response401 = new Response();
        response401.description("Unauthorized");
        Response response403 = new Response();
        response403.description("Forbidden");
        Response response404 = new Response();
        response404.description("Not Found");
        swagger.response("401", response401);
        swagger.response("403", response403);
        swagger.response("404", response404);

        for (var table : this.namedTables.values()) {
            // api 接口列表
            var tag = new Tag();
            tag.name(table.getAlias());
            tag.description(table.getRemark());
            swagger.tag(tag);

            ModelImpl model = new ModelImpl();
            for (var column : table.getColumns()) {
                switch (column.getColumnDefinition().getDefinitionName()) {
                    case "DATE":
                        model.property(column.getName(), new DateProperty().description(column.getRemark()));
                        break;
                    case "TIME":
                    case "TIMESTAMP":
                    case "DATETIME":
                        model.property(column.getName(), new DateTimeProperty().description(column.getRemark()));
                        break;
                    case "CHAR":
                    case "VARCHAR":
                    case "NCHAR":
                    case "NVARCHAR":
                        model.property(column.getName(), new StringProperty().description(column.getRemark()));
                        break;
                    case "BOOLEAN":
                    case "BOOL":
                        model.property(column.getName(), new BooleanProperty().description(column.getRemark()));
                        break;
                    case "BIGINT":
                    case "LONG":
                    case "BIGSERIAL":
                        model.property(column.getName(), new LongProperty().description(column.getRemark()));
                        break;
                    case "SMALLINT":
                    case "TINYINT":
                    case "INTEGER":
                    case "INT":
                        model.property(column.getName(), new IntegerProperty().description(column.getRemark()));
                        break;
                    case "FLOAT":
                        model.property(column.getName(), new FloatProperty().description(column.getRemark()));
                        break;
                    case "DOUBLE":
                        model.property(column.getName(), new DoubleProperty().description(column.getRemark()));
                        break;
                    case "NUMERIC":
                    case "DECIMAL":
                        model.property(column.getName(), new DecimalProperty().description(column.getRemark()));
                        break;
                    default:
                        log.warn("没有考虑到的类型：{}", column.getColumnDefinition().getDefinitionName());
                        model.property(column.getName() + ":" + column.getColumnDefinition().getDefinitionName(), new StringProperty().description(column.getRemark()));
                        break;

                }
            }
            swagger.addDefinition(table.getAlias(), model);

            // api 路径
            Path path = new Path();
            Path path1 = new Path();
            for (var acl : this.namedAcl.get(table.getName())) {
                switch (acl) {
                    case QueryByKey:
                        path1.get(queryByKeyOperation(table, model));
                    case DeleteByKey:
                        path1.delete(deleteByKeyOperation(table, model));
                    case UpdateByKey:
                        path1.post(updateByKeyOperation(table, model));
                    case QueryList:
                        path.get(queryListOperation(table, model));
                    case Delete:
                        path.delete(deleteOperation(table, model));
                        break;
                    case Create:
                        path.put(createOperation(table, model));
                    case Update:
                        path.post(updateOperation(table, model));

                    default:

                        break;
                }
            }
            swagger.path("/v2/api/" + table.getAlias(), path);
            swagger.path("/v2/api/" + table.getAlias() + "/{key}", path);
        }

        var path = new Path();
        path.post(queryOperation());
        swagger.path("/v2/api/query", path);

        return swagger;
    }

    private Operation queryOperation() {
        Operation operation = new Operation();
        operation.tag("query");
        operation.summary("query multi-resources by fql");
        operation.description("");
        operation.operationId("query");
        operation.consumes("application/json");
        operation.produces("*/*");

        // parameter
        operation.parameter(new BodyParameter().name("query").description("fql").example("application/json", ""));
        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new ArrayProperty().description("列表").items(new MapProperty()));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    private Operation queryListOperation(Table table, ModelImpl model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("query list from " + table.getAlias() + " by custom conditions");
        operation.description(table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.QueryList.name());
        operation.consumes("application/json");
        operation.produces("*/*");

        // parameter
        operation.parameter(new PathParameter().name("select").description("列名逗号隔开，不传默认为*").required(false));
        operation.parameter(new PathParameter().name("where").description("查询条件"));
        operation.parameter(new PathParameter().name("order").description("排序规则").required(false));
        operation.parameter(new PathParameter().name("group").description("分组规则"));
        operation.parameter(new PathParameter().name("page").description("分页-页").required(false).type("int32"));
        operation.parameter(new PathParameter().name("size").description("分页-页").required(false).type("int32"));
        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new ArrayProperty().description("列表").items(new ObjectProperty(model.getProperties())));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    private Operation deleteByKeyOperation(Table table, ModelImpl model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("delete" + table.getAlias() + "by key");
        operation.description(table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.Delete.name());
        operation.consumes("application/json");
        operation.produces("*/*");
        // parameter
        operation.parameter(new PathParameter().name("id").description("唯一编号").required(true));
        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new BooleanProperty().description("data"));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    private Operation deleteOperation(Table table, ModelImpl model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("delete" + table.getAlias() + "by key");
        operation.description(table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.Delete.name());
        operation.consumes("application/json");
        operation.produces("*/*");
        // parameter
        operation.parameter(new PathParameter().name("where").description("条件").required(true));
        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new BooleanProperty().description("data"));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    private Operation updateByKeyOperation(Table table, ModelImpl model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("update " + table.getAlias() + " using json object by key");
        operation.description(table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.Update.name());
        operation.consumes("application/json");
        operation.produces("*/*");
        // parameter
        operation.parameter(new PathParameter().name("id").description("唯一编号").required(true));
        var parameter = new BodyParameter();
        parameter.name("data");
        parameter.description("属性名:属性值的键值对");
        parameter.schema(model);
        parameter.setRequired(true);
        operation.parameter(parameter);
        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new BooleanProperty().description("data"));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    private Operation updateOperation(Table table, ModelImpl model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("update " + table.getAlias() + " using json object by custom conditions");
        operation.description(table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.Update.name());
        operation.consumes("application/json");
        operation.produces("*/*");
        // parameter
        operation.parameter(new PathParameter().name("where").description("条件").required(true));
        var parameter = new BodyParameter();
        parameter.name("data");
        parameter.description("属性名:属性值的键值对");
        parameter.schema(model);
        parameter.setRequired(true);
        operation.parameter(parameter);
        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new BooleanProperty().description("data"));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    private Operation queryByKeyOperation(Table table, Model model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("query one data from " + table.getAlias() + " by key");
        operation.description(table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.QueryByKey.name());
        operation.consumes("application/json");
        operation.produces("*/*");
        // parameter
        operation.parameter(new PathParameter().name("id").description("唯一编号").required(true));

        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new ObjectProperty(model.getProperties()));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    /**
     * 增加
     *
     * @param table
     * @param model
     */
    private Operation createOperation(Table table, Model model) {
        Operation operation = new Operation();
        operation.tag(table.getAlias());
        operation.summary("新增" + table.getRemark());
        operation.description("新增" + table.getRemark());
        operation.operationId(JdbcApiProperties.Acl.Create.name());
        operation.consumes("application/json");
        operation.produces("*/*");
        // parameter
        var parameter = new BodyParameter();
        parameter.name("data");
        parameter.description("属性名:属性值的键值对");
        parameter.schema(model);
        parameter.setRequired(true);
        operation.parameter(parameter);

        // response
        Response response = new Response();
        response.description("OK");
        var responseSchema = new ModelImpl();
        responseSchema.description("");
        responseSchema.name("data");
        responseSchema.property("data", new BooleanProperty().description("data"));
        responseSchema.property("code", new IntegerProperty().description("code"));
        response.setResponseSchema(responseSchema);
        return operation.response(200, response);
    }

    @Getter
    private Map<String, Table> namedTables;

    @Getter
    private Map<String, JdbcApiProperties.Acl[]> namedAcl;

    @Getter
    private Map<String, Map<String, Column>> namedColumns;

    @Override
    public void afterPropertiesSet() throws Exception {
        for (var entry : this.properties.getDatabases().entrySet()) {
            var catalog = entry.getValue().getCatalog();
            var schema = entry.getValue().getSchema();
            var list = this.listAllTables(catalog, schema);
            var tables = rename(list, entry.getValue());
            this.namedTables = tables.stream().collect(Collectors.toMap(
                    k -> k.getAlias(), v -> v
            ));
            this.namedColumns = namedTables.values().stream().collect(Collectors.toMap(
                    k -> k.getAlias(), v -> {
                        var map = Arrays.stream(v.getColumns()).collect(Collectors.toMap(
                                m -> m.getAlias(), m -> m
                        ));
                        return map;
                    }
            ));
        }
    }


    List<Table> rename(List<Table> list, JdbcApiProperties.DatabaseRule db) {
        for (var table : list) {
            for (var entry : db.getTables().entrySet()) {
                AntPathMatcher pathMatcher = new AntPathMatcher();
                if (pathMatcher.match(entry.getKey(), table.getName())) {
                    namedAcl.put(table.getName(), entry.getValue().getAcl());
                    var tableName = table.getName();
                    var newTable = rename(table.getName(), entry.getValue());
                    table.as(newTable);
                    for (var column : table.getColumns()) {
                        var columnName = column.getName();
                        var newColumn = rename(column.getName(), entry.getValue().getColumn());
                        column.as(newColumn);
                    }
                }
            }

            if (Strings.isNullOrEmpty(table.getAlias())) {
                table.as(table.getName());
            }
            for (var column : table.getColumns()) {
                if (Strings.isNullOrEmpty(column.getAlias())) {
                    column.as(column.getName());
                }
            }
        }
        return list;
    }

    String rename(String name, JdbcApiProperties.TableNameRule rule) {
        var newName = name;
        for (var i : rule.getRemoves()) {
            if (newName.startsWith(i.getPrefix())) {
                newName = newName.substring(i.getPrefix().length());
            }
            if (newName.endsWith(i.getSuffix())) {
                newName.substring(0, newName.length() - i.getSuffix().length());
            }
        }
        for (var i : rule.getAppends()) {
            newName = i.getPrefix() + newName + i.getSuffix();
        }
        for (var i : rule.getReplaces().entrySet()) {
            newName.replace(i.getKey(), i.getValue());
        }
        return newName;
    }

    String rename(String name, JdbcApiProperties.ColumnNameRule rule) {
        var newName = name;
        for (var i : rule.getRemoves()) {
            if (newName.startsWith(i.getPrefix())) {
                newName = newName.substring(i.getPrefix().length());
            }
            if (newName.endsWith(i.getSuffix())) {
                newName.substring(0, newName.length() - i.getSuffix().length());
            }
        }
        for (var i : rule.getAppends()) {
            newName = i.getPrefix() + newName + i.getSuffix();
        }

        for (var i : rule.getReplaces().entrySet()) {
            newName.replace(i.getKey(), i.getValue());
        }
        return newName;
    }


}
