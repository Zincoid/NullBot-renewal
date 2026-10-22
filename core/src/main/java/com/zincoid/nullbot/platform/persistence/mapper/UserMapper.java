package com.zincoid.nullbot.platform.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.zincoid.nullbot.platform.persistence.po.User;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
