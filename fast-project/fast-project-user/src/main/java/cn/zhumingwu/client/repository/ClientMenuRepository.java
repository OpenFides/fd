package cn.zhumingwu.client.repository;

import cn.zhumingwu.client.entity.ClientMenu;
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
public class ClientMenuRepository {

    static Cache<String, Expression> local = CacheBuilder.newBuilder().maximumSize(1024)
        .expireAfterAccess(10L, TimeUnit.MINUTES).removalListener(removalNotification -> { })
        .build();


    @Resource
    JdbcTemplate jdbcTemplate;
    private final ExpressionParser parser = new SpelExpressionParser();
    @Resource
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<ClientMenu> page(int index, int pageSize, String order, String where, Object... param) {
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageSize * index);
        String sql = "SELECT menu_id,\ncid,\nparent_id,\npath,\nname,\ncontent,\nsort,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\nFROM t_client_menu " +
             where +
             order +
             limit;
        return this.jdbcTemplate.query(sql, this::extractData, param);
    }


    public List<ClientMenu> query(String where, Object... param) {
        String sql = "SELECT menu_id,\ncid,\nparent_id,\npath,\nname,\ncontent,\nsort,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\nFROM t_client_menu " + where;
        return this.jdbcTemplate.query(sql, this::extractData, param);
    }

    public List<ClientMenu> queryByPrimaryKey(Object key) {
        String sql = "SELECT menu_id,\ncid,\nparent_id,\npath,\nname,\ncontent,\nsort,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\nFROM t_client_menu  WHERE menu_id=? and \ncid=? \n;";
        return this.jdbcTemplate.query(sql, this::extractData, key);
    }

    public int insert(ClientMenu... entities) {
        String sql = "INSERT INTO t_client_menu (menu_id,\ncid,\nparent_id,\npath,\nname,\ncontent,\nsort,\ncreate_time,\nupdate_time,\ncreate_by,\nupdate_by,\nstatus\n) VALUES (\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?,\n?\n)";
        int result = 0;
        for (var entity : entities) {
            var id = entity.getId();
            var parentId = entity.getParentId();
            var path = entity.getPath();
            var name = entity.getName();
            var content = entity.getContent();
            var sortNo = entity.getSortNo();
            var cid = entity.getCid();
            var createdTime = entity.getCreatedTime();
            var updatedTime = entity.getUpdatedTime();
            var createdBy = entity.getCreatedBy();
            var updatedBy = entity.getUpdatedBy();
            var status = entity.getStatus();
            result += this.jdbcTemplate.update( sql, id,cid,parentId,path,name,content,sortNo,createdTime,updatedTime,createdBy,updatedBy,status);
        }
        return result;
    }

    public int update(ClientMenu... entities) {

        String sql = "UPDATE t_client_menu SET parent_id=?,\npath=?,\nname=?,\ncontent=?,\nsort=?,\ncreate_time=?,\nupdate_time=?,\ncreate_by=?,\nupdate_by=?,\nstatus=?\n WHERE menu_id=? and \ncid=? \n;";
        int result = 0;
        for (var entity : entities) {
            var id = entity.getId();
            var parentId = entity.getParentId();
            var path = entity.getPath();
            var name = entity.getName();
            var content = entity.getContent();
            var sortNo = entity.getSortNo();
            var cid = entity.getCid();
            var createdTime = entity.getCreatedTime();
            var updatedTime = entity.getUpdatedTime();
            var createdBy = entity.getCreatedBy();
            var updatedBy = entity.getUpdatedBy();
            var status = entity.getStatus();
            result += this.jdbcTemplate.update( sql, parentId,path,name,content,sortNo,createdTime,updatedTime,createdBy,updatedBy,status,id,cid);
        }
        return result;
    }

    public int delete(int... ids) {
        String sql = "UPDATE t_client_menu SET status = -1 WHERE menu_id=? and \ncid=? \n;";
        int result = 0;
        for (var id : ids) {
            result += this.jdbcTemplate.update(sql, id);
        }
        return result;
    }

    public int deleteAll(Integer... ids) {
        String sql = "DELETE FROM t_client_menu WHERE menu_id=? and \ncid=? \n;";
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

    List<ClientMenu> extractData(ResultSet rs)  {
        List<ClientMenu> list = new ArrayList<>();
		try {
            while (rs.next()) {
                ClientMenu entity = new ClientMenu();
                entity.setId(rs.getLong("menu_id"));
                entity.setParentId(rs.getLong("parent_id"));
                entity.setPath(rs.getString("path"));
                entity.setName(rs.getString("name"));
                entity.setContent(rs.getString("content"));
                entity.setSortNo(rs.getInt("sort"));
                entity.setCid(rs.getInt("cid"));
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
