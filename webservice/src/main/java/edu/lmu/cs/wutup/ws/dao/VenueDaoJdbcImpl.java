package edu.lmu.cs.wutup.ws.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import edu.lmu.cs.wutup.ws.exception.NoSuchVenueException;
import edu.lmu.cs.wutup.ws.exception.VenueExistsException;
import edu.lmu.cs.wutup.ws.model.Comment;
import edu.lmu.cs.wutup.ws.model.Venue;

@Repository
public class VenueDaoJdbcImpl implements VenueDao {

    private static final String SELECT_VENUE = "select v.* from venue v ";
    private static final String SELECT_COMMENT = "select ec.*, u.* from venue_comment ec join user u on (ec.authorId = u.id)";
    private static final String PAGINATION = "limit ? offset ?";

    private static final String FIND_COMMENTS_SQL = SELECT_COMMENT + " where ec.venueId = ? " + PAGINATION;
    private static final String CREATE_SQL = "insert into venue (name, address, latitude, longitude) values (?,?,?,?)";
    private static final String UPDATE_SQL = "update venue set name=?, address=?, latitude=?, longitude=? where id=?";
    private static final String FIND_BY_ID_SQL = SELECT_VENUE + " where v.id=?";
    private static final String FIND_ALL_SQL = SELECT_VENUE + " " + PAGINATION;
    private static final String FIND_BY_ADDRESS_SQL = SELECT_VENUE + " where v.address=? " + PAGINATION;
    private static final String DELETE_SQL = "delete from venue where id=?";
    private static final String COUNT_SQL = "select count(*) from venue";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public void createVenue(Venue loc) {
        try {
            jdbcTemplate.update(CREATE_SQL, loc.getName(), loc.getAddress(), loc.getLatitude(), loc.getLongitude());
        } catch (DuplicateKeyException ex) {
            throw new VenueExistsException();
        }
    }

    @Override
    public void updateVenue(Venue loc) {
        int rowsUpdated = jdbcTemplate.update(UPDATE_SQL, loc.getName(), loc.getAddress(), loc.getLatitude(),
                loc.getLongitude(), loc.getId());
        if (rowsUpdated == 0) {
            throw new NoSuchVenueException();
        }
    }

    @Override
    public void deleteVenue(int venueId) {
        int rowsUpdated = jdbcTemplate.update(DELETE_SQL, venueId);
        if (rowsUpdated == 0) {
            throw new NoSuchVenueException();
        }
    }

    @Override
    public Venue findVenueById(int id) {
        try {
            return jdbcTemplate.queryForObject(FIND_BY_ID_SQL, new Object[]{id}, venueRowMapper);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new NoSuchVenueException();
        }
    }

    @Override
    public List<Venue> findVenuesByAddress(String address, int pageNumber, int pageSize) {
        return jdbcTemplate.query(FIND_BY_ADDRESS_SQL, new Object[]{address, pageSize, pageNumber * pageSize},
                venueRowMapper);
    }

    @Override
    public List<Venue> findVenuesByPropertyMap(String propertyMap, int pageNumber, int pageSize) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Venue> findAllVenues(int pageNumber, int pageSize) {
        return jdbcTemplate.query(FIND_ALL_SQL, new Object[]{pageSize, pageNumber * pageSize}, venueRowMapper);
    }

    @Override
    public int findNumberOfVenues() {
        return jdbcTemplate.queryForInt(COUNT_SQL);
    }

    @Override
    public void addComment(Integer venueId, Comment comment) {
        CommentDaoUtils.addComment(jdbcTemplate, "venue", venueId, comment);
    }

    @Override
    public void updateComment(Integer commentId, Comment c) {
        CommentDaoUtils.updateComment(jdbcTemplate, "venue", commentId, c);
    }

    @Override
    public void deleteComment(int venueId, int commentId) {
        CommentDaoUtils.deleteComment(jdbcTemplate, "venue", venueId, commentId);
    }

    @Override
    public List<Comment> findComments(int venueId, int pageNumber, int pageSize) {
        return CommentDaoUtils.findCommentableObjectComments(jdbcTemplate, FIND_COMMENTS_SQL, venueId, pageNumber,
                pageSize);
    }

    private static RowMapper<Venue> venueRowMapper = new RowMapper<Venue>() {
        public Venue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Venue(rs.getInt("id"), rs.getString("name"), rs.getString("address"), rs.getDouble("latitude"),
                    rs.getDouble("longitude"), null);
        }
    };

    // private static RowMapper<ResultSet> venuePropertiesRowMapper = new RowMapper<ResultSet>() {
    // public ResultSet mapRow(ResultSet rs, int rowNum) throws SQLException {
    // return new ResultSet(rs.getString(0), rs.getString(1));
    // }
    // };

}
