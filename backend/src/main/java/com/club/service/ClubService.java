package com.club.service;

import com.club.dao.ClubDao;
import com.club.dao.NoticeDao;
import com.club.entity.Club;
import com.club.entity.Notice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//社团业务
public class ClubService {

    private final ClubDao clubDao = new  ClubDao();
    private final NoticeDao noticeDao = new  NoticeDao();

    /**
     * 学生侧 onlyActive=true，管理员侧 onlyActive=false
     */
    public Map<String,Object> search(String keyword,String category, int page, int size, boolean onlyActive) {
        if (page < 1) page = 1;
        if (size < 1 || size > 100) size = 10;
        int offset = (page - 1) * size;

        List<Club> records = clubDao.search(keyword,category,offset,size,onlyActive);
        int total = clubDao.countSearch(keyword, category, onlyActive);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public Map<String, Object> detail(Long id) {
        Club club = clubDao.findById(id);
        if (club == null) throw new IllegalArgumentException("社团不存在");
        List<Notice> notices = noticeDao.listByClubId(id, 3);

        Map<String, Object> result = new HashMap<>();
        result.put("club", club);
        result.put("notices", notices);
        return result;
    }

    public Long create(Club club) {
        validate(club);
        clubDao.insert(club);
        return club.getId();
    }

    public void update(Club club) {
        if (club.getId() == null) throw new IllegalArgumentException("缺少 id");
        validate(club);
        if (clubDao.update(club) != 1) throw new IllegalArgumentException("社团不存在或未变化");
    }

    public void deactivate(Long id) {
        if (clubDao.deactivate(id) !=1) throw new IllegalArgumentException("社团不存在");
    }

    public Club findById(Long id) {
        return clubDao.findById(id);
    }

    public int countAll() {
        return clubDao.countSearch(null,null,false);
    }

    private void validate(Club club) {
        if (club.getName() == null || club.getName().isBlank()) {
            throw new IllegalArgumentException("社团名不能为空");
        }
        if (club.getCategory() == null || club.getCategory().isBlank()) {
            throw new IllegalArgumentException("分类不能为空");
        }
        if (club.getMaxMembers() == null || club.getMaxMembers() <= 0) {
            throw new IllegalArgumentException("人数上限必须 > 0");
        }
        if (club.getLeader() == null || club.getLeader().isBlank()) {
            throw new IllegalArgumentException("负责人不能为空");
        }
        if (club.getIntro() == null || club.getIntro().isBlank()) {
            club.setIntro(null);
        }
        if (club.getIsActive() == null || club.getIsActive() == 0) {
            club.setIsActive(1);
        }
    }
}
