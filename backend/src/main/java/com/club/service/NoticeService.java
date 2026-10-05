package com.club.service;

import com.club.dao.NoticeDao;
import com.club.entity.Notice;

import java.util.List;

//公告业务
public class NoticeService {

    private final NoticeDao noticeDao = new NoticeDao();

    public List<Notice> list(Long clubId) {
        if (clubId == null) {
            return noticeDao.listAllWithClub();
        }
        return noticeDao.listByClubId(clubId,100);
    }

    public Long publish(Long clubId, String title, String content, Long publisherId) {
        if (clubId == null) {
            throw new IllegalArgumentException("社团 id 不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        if (title.length() > 64) {
            throw new IllegalArgumentException("标题不超 64 字");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("正文不能为空");
        }

        Notice n = new Notice();
        n.setClubId(clubId);
        n.setTitle(title);
        n.setContent(content.trim());
        n.setPublisherId(publisherId);
        noticeDao.insert(n);
        return n.getId();
    }
}
