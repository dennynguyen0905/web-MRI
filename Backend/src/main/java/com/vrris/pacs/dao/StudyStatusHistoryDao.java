package com.vrris.pacs.dao;

import com.vrris.pacs.model.StudyStatusHistory;

import java.util.List;

public interface StudyStatusHistoryDao extends BaseDao<StudyStatusHistory, Long> {

    List<StudyStatusHistory> findByStudyIdOrderByChangedAtAsc(Long studyId);
}
