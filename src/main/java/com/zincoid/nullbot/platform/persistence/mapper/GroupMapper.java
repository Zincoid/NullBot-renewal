package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.zincoid.nullbot.platform.persistence.po.Group;

@Mapper
public interface GroupMapper extends BaseMapper<Group> {
}
