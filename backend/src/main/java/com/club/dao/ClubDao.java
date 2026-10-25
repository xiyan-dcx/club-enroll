package com.club.dao;
import com.club.entity.Club;import com.club.mapper.*;import com.club.util.*;import java.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
public class ClubDao {
 private ClubMapper mapper(){return Beans.get(ClubMapper.class);}
 private QueryWrapper<Club> query(String keyword,String category,boolean active){var q=new QueryWrapper<Club>();if(active)q.eq("is_active",1);if(keyword!=null&&!keyword.isBlank())q.like("name",keyword.trim());if(category!=null&&!category.isBlank())q.eq("category",category);return q;}
 public List<Club> search(String k,String cat,int offset,int size,boolean active){return mapper().selectList(query(k,cat,active).orderByDesc("current_members").orderByAsc("id").last("LIMIT "+size+" OFFSET "+offset));}
 public int countSearch(String k,String cat,boolean active){return mapper().selectCount(query(k,cat,active)).intValue();}
 public Club findById(Long id){return mapper().selectById(id);}
 public int insert(Club c){c.setCurrentMembers(0);return mapper().insert(c);}
 public int update(Club c){return Sql.transaction(m->Sql.update(m,"UPDATE club SET name=?,category=?,intro=?,leader=?,max_members=?,is_active=? WHERE id=? AND current_members<=?",c.getName(),c.getCategory(),c.getIntro(),c.getLeader(),c.getMaxMembers(),c.getIsActive(),c.getId(),c.getMaxMembers()));}
 public int deactivate(Long id){return Sql.transaction(m->Sql.update(m,"UPDATE club SET is_active=0 WHERE id=?",id));}
 public int incrementCurrentMembers(SqlMapper c,Long id){return Sql.update(c,"UPDATE club SET current_members=current_members+1 WHERE id=? AND is_active=1 AND current_members<max_members",id);}
}