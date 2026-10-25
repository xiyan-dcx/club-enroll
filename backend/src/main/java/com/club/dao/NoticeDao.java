package com.club.dao;
import com.club.entity.Notice;import com.club.util.Sql;import java.util.*;
public class NoticeDao {
 public int insert(Notice n){n.setId(Sql.transaction(c->Sql.insert(c,"INSERT INTO notice(club_id,title,content,publisher_id) VALUES(?,?,?,?)",n.getClubId(),n.getTitle(),n.getContent(),n.getPublisherId())));return 1;}
 private List<Notice> list(String clause,Object...args){return Sql.transaction(c->Sql.rows(c,"SELECT n.*,c.name AS club_name FROM notice n JOIN club c ON c.id=n.club_id "+clause,args).stream().map(r->Sql.entity(r,Notice.class)).toList());}
 public List<Notice> listAllWithClub(){return list("ORDER BY n.created_at DESC");}
 public List<Notice> listByClubId(Long id,int limit){return list("WHERE n.club_id=? ORDER BY n.created_at DESC LIMIT ?",id,limit);}
 public int countAll(){return Sql.count("SELECT COUNT(*) n FROM notice");}
}