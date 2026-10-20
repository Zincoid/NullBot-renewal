package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zincoid.nullbot.platform.persistence.po.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
