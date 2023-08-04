package cn.zhumingwu.client.repository;

import cn.zhumingwu.client.entity.App;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AppRepository {

    static Cache<String, Expression> local = CacheBuilder.newBuilder().maximumSize(1024)
        .expireAfterAccess(10L, TimeUnit.MINUTES).removalListener(removalNotification -> { })
        .build();


    @Resource
    JdbcTemplate jdbcTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    @Resource
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<App> page(int index, int pageSize, String order, String where, Object... param) {
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageSize * index);
        String sql = "SELECT app_id,\nname,\ndescription,\nicon,\ncode,\ntype,\nurl,\nvendor_id,\nissue_ts,\nexpires_in,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\nFROM t_system_app " +
             where +
             order +
             limit;
        return this.jdbcTemplate.query(sql, this::extractData, param);
    }


    public List<App> query(String where, Object... param) {
        String sql = "SELECT app_id,\nname,\ndescription,\nicon,\ncode,\ntype,\nurl,\nvendor_id,\nissue_ts,\nexpires_in,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\nFROM t_system_app " + where;
        return this.jdbcTemplate.query(sql, this::extractData, param);
    }

    public List<App> queryByPrimaryKey(Object key) {
        String sql = "SELECT app_id,\nname,\ndescription,\nicon,\ncode,\ntype,\nurl,\nvendor_id,\nissue_ts,\nexpires_in,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\nFROM t_system_app  WHERE app_id=? \n;";
        return this.jdbcTemplate.query(sql, this::extractData, key);
    }

    public int insert(App... entities) {
        String sql = "INSERT INTO t_system_app (app_id,\nname,\ndescription,\nicon,\ncode,\ntype,\nurl,\nvendor_id,\nissue_ts,\nexpires_in,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\n) VALUES (\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?\n)";
        int result = 0;
        for (var entity : entities) {
            var id = entity.getId();
            var name = entity.getName();
            var description = entity.getDescription();
            var icon = entity.getIcon();
            var code = entity.getCode();
            var type = entity.getType();
            var url = entity.getUrl();
            var vendorId = entity.getVendorId();
            var issueTime = entity.getIssueTime();
            var expiresIn = entity.getExpiresIn();
            var createdTime = entity.getCreatedTime();
            var updatedTime = entity.getUpdatedTime();
            var createdBy = entity.getCreatedBy();
            var updatedBy = entity.getUpdatedBy();
            var status = entity.getStatus();
            result += this.jdbcTemplate.update( sql, id,name,description,icon,code,type,url,vendorId,issueTime,expiresIn,createdTime,updatedTime,createdBy,updatedBy,status);
        }
        return result;
    }

    public int update(App... entities) {

        String sql = "UPDATE t_system_app SET name=?,\ndescription=?,\nicon=?,\ncode=?,\ntype=?,\nurl=?,\nvendor_id=?,\nissue_ts=?,\nexpires_in=?,\ncreate_time=?,\nupdate_time=?,\ncreate_by=?,\nupdate_by=?,\nstatus=?\n WHERE app_id=? \n;";
        int result = 0;
        for (var entity : entities) {
            var id = entity.getId();
            var name = entity.getName();
            var description = entity.getDescription();
            var icon = entity.getIcon();
            var code = entity.getCode();
            var type = entity.getType();
            var url = entity.getUrl();
            var vendorId = entity.getVendorId();
            var issueTime = entity.getIssueTime();
            var expiresIn = entity.getExpiresIn();
            var createdTime = entity.getCreatedTime();
            var updatedTime = entity.getUpdatedTime();
            var createdBy = entity.getCreatedBy();
            var updatedBy = entity.getUpdatedBy();
            var status = entity.getStatus();
            result += this.jdbcTemplate.update( sql, name,description,icon,code,type,url,vendorId,issueTime,expiresIn,createdTime,updatedTime,createdBy,updatedBy,status,id);
        }
        return result;
    }

    public int delete(int... ids) {
        String sql = "UPDATE t_system_app SET status = -1 WHERE app_id=? \n;";
        int result = 0;
        for (var id : ids) {
            result += this.jdbcTemplate.update(sql, id);
        }
        return result;
    }

    public int deleteAll(Integer... ids) {
        String sql = "DELETE FROM t_system_app WHERE app_id=? \n;";
        var list = Arrays.asList(ids);
        int result = 0;
        for (var partition : Lists.partition(list, 1000)) {
            result += Arrays.stream(this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setInt(1, partition.get(i));
                }
                @Override
                public int getBatchSize() {
                    return partition.size();
                }
            })).sum();
        }
        return result;
    }

    public <T> T getNamedQuery(String sql, ResultSetExtractor<T> rse, Map<String, ?> paramMap) {
        return this.namedParameterJdbcTemplate.query(sql, paramMap, rse);
    }

    public <T> T getQuery(String sql, ResultSetExtractor<T> rse, @Nullable Object... args) {
        return this.jdbcTemplate.query(sql, rse, args);
    }

    public <T> T getExpQuery(String spel, ResultSetExtractor<T> rse, Map<String, ?> paramMap) {
        var expression = local.getIfPresent(spel);
        if (expression == null) {
            expression = this.parser.parseExpression(spel);
            local.put(spel, expression);
        }
        var sql = expression.getValue(paramMap, String.class);
        assert sql != null;
        return this.jdbcTemplate.query(sql, rse);
    }

    List<App> extractData(ResultSet rs)  {
        List<App> list = new ArrayList<>();
		try {
            while (rs.next()) {
                App entity = new App();
                entity.setId(rs.getLong("app_id"));
                entity.setName(rs.getString("name"));
                entity.setDescription(rs.getString("description"));
                entity.setIcon(rs.getString("icon"));
                entity.setCode(rs.getString("code"));
                // todo: AppType ;
                entity.setUrl(rs.getString("url"));
                entity.setVendorId(rs.getLong("vendor_id"));
                entity.setIssueTime(rs.getDate("issue_ts"));
                entity.setExpiresIn(rs.getLong("expires_in"));
                entity.setCreatedTime(rs.getDate("create_time"));
                entity.setUpdatedTime(rs.getDate("update_time"));
                entity.setCreatedBy(rs.getLong("create_by"));
                entity.setUpdatedBy(rs.getLong("update_by"));
                // todo: Status ;
                list.add(entity);
            }
        } catch (SQLException e) {
            log.error("error", e);
        }
        return list;
    }
}
