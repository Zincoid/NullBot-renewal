package com.zincoid.nullbot.adapter.qq.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * QQ 官方机器人 API 数据结构。
 * <p>QQ official bot API data structures.</p>
 */
public final class QqModels {

    private QqModels() {
    }

    /**
     * Access token 请求。
     * <p>Access token request.</p>
     *
     * @param appId        the open platform app id
     * @param clientSecret the open platform client secret
     */
    public record TokenRequest(String appId, String clientSecret) {
    }

    /**
     * WebSocket 网关响应。
     * <p>WebSocket gateway response.</p>
     *
     * @param url    the gateway url
     * @param shards the suggested shard count
     */
    public record GatewayResponse(String url, Integer shards) {
    }

    /**
     * WebSocket 下行数据包。
     * <p>WebSocket downstream payload.</p>
     *
     * @param op the opcode
     * @param s  the sequence number
     * @param t  the event name
     * @param d  the event data
     * @param id the event id
     */
    public record Payload(
            int op,
            Long s,
            String t,
            JsonNode d,
            String id
    ) {
    }

    /**
     * READY 事件数据。
     * <p>READY event data.</p>
     *
     * @param sessionId the session id
     * @param user      the bot user
     * @param version   the gateway version
     */
    public record ReadyData(
            @JsonProperty("session_id") String sessionId,
            BotUser user,
            Integer version
    ) {
    }

    /**
     * 机器人用户。
     * <p>Bot user.</p>
     *
     * @param id       the user id
     * @param username the username
     * @param avatar   the avatar url
     * @param bot      whether the user is a bot
     */
    public record BotUser(String id, String username, String avatar, Boolean bot) {
    }

    /**
     * 消息发送者。
     * <p>Message author.</p>
     *
     * @param id           the user id
     * @param username     the username
     * @param bot          whether the author is a bot
     * @param memberOpenid the group member openid
     * @param userOpenid   the C2C user openid
     * @param unionOpenid  the union openid
     * @param memberRole   the group member role
     */
    public record Author(
            String id,
            String username,
            Boolean bot,
            @JsonProperty("member_openid") String memberOpenid,
            @JsonProperty("user_openid") String userOpenid,
            @JsonProperty("union_openid") String unionOpenid,
            @JsonProperty("member_role") String memberRole
    ) {
    }

    /**
     * 被提及的用户。
     * <p>Mentioned user.</p>
     *
     * @param id           the user id
     * @param username     the username
     * @param bot          whether the user is a bot
     * @param memberOpenid the group member openid
     * @param userOpenid   the C2C user openid
     * @param unionOpenid  the union openid
     * @param isYou        whether the mentioned user is the bot
     * @param scope        the mention scope
     */
    public record Mention(
            String id,
            String username,
            Boolean bot,
            @JsonProperty("member_openid") String memberOpenid,
            @JsonProperty("user_openid") String userOpenid,
            @JsonProperty("union_openid") String unionOpenid,
            @JsonProperty("is_you") Boolean isYou,
            String scope
    ) {
    }

    /**
     * 消息附件。
     * <p>Message attachment.</p>
     *
     * @param contentType the MIME type
     * @param filename    the original filename
     * @param url         the download url
     * @param size        the file size
     * @param width       the image width
     * @param height      the image height
     * @param voiceWavUrl the voice wav url
     */
    public record Attachment(
            @JsonProperty("content_type") String contentType,
            String filename,
            String url,
            Long size,
            Integer width,
            Integer height,
            @JsonProperty("voice_wav_url") String voiceWavUrl
    ) {
    }

    /**
     * 群消息事件数据。
     * <p>Group message event data.</p>
     *
     * @param id           the message id
     * @param author       the sender
     * @param content      the text content
     * @param groupOpenid  the group openid
     * @param messageType  the message type
     * @param messageScene the message scene
     * @param attachments  the attachments
     * @param mentions     the mentioned users
     * @param arkData      the ark card data
     * @param msgElements  the nested message elements
     * @param timestamp    the RFC3339 timestamp
     */
    public record GroupMessage(
            String id,
            Author author,
            String content,
            @JsonProperty("group_openid") String groupOpenid,
            @JsonProperty("message_type") Integer messageType,
            @JsonProperty("message_scene") JsonNode messageScene,
            List<Attachment> attachments,
            List<Mention> mentions,
            @JsonProperty("ark_data") JsonNode arkData,
            @JsonProperty("msg_elements") JsonNode msgElements,
            String timestamp
    ) {
    }

    /**
     * C2C 私聊消息事件数据。
     * <p>C2C message event data.</p>
     *
     * @param id           the message id
     * @param author       the sender
     * @param content      the text content
     * @param messageType  the message type
     * @param messageScene the message scene
     * @param attachments  the attachments
     * @param mentions     the mentioned users
     * @param arkData      the ark card data
     * @param msgElements  the nested message elements
     * @param timestamp    the RFC3339 timestamp
     */
    public record C2cMessage(
            String id,
            Author author,
            String content,
            @JsonProperty("message_type") Integer messageType,
            @JsonProperty("message_scene") JsonNode messageScene,
            List<Attachment> attachments,
            List<Mention> mentions,
            @JsonProperty("ark_data") JsonNode arkData,
            @JsonProperty("msg_elements") JsonNode msgElements,
            String timestamp
    ) {
    }

    /**
     * 发送消息请求。
     * <p>Send message request.</p>
     *
     * @param msgType the message type
     * @param content the text content
     */
    public record SendRequest(
            @JsonProperty("msg_type") int msgType,
            String content
    ) {
    }

    /**
     * 发送消息响应。
     * <p>Send message response.</p>
     *
     * @param id        the message id
     * @param timestamp the send timestamp
     */
    public record SendResponse(String id, JsonNode timestamp) {
    }

    /**
     * 上传文件请求。
     * <p>Upload file request.</p>
     *
     * @param fileType   the media type
     * @param url        the source url
     * @param fileData   the base64 file data
     * @param fileName   the file name
     * @param srvSendMsg whether the platform should send the file directly
     */
    public record UploadRequest(
            @JsonProperty("file_type") int fileType,
            String url,
            @JsonProperty("file_data") String fileData,
            @JsonProperty("file_name") String fileName,
            @JsonProperty("srv_send_msg") boolean srvSendMsg
    ) {
    }

    /**
     * 上传文件响应。
     * <p>Upload file response.</p>
     *
     * @param fileInfo the file info for sending
     * @param fileUuid the file uuid
     * @param ttl      the file info lifetime
     * @param id       the message id when sent directly
     */
    public record UploadResponse(
            @JsonProperty("file_info") String fileInfo,
            @JsonProperty("file_uuid") String fileUuid,
            Integer ttl,
            String id
    ) {
    }

    /**
     * 群基本信息。
     * <p>Group basic info.</p>
     *
     * @param groupOpenid   the group openid
     * @param groupName     the group name
     * @param groupMemberNum the member count
     */
    public record GroupInfoData(
            @JsonProperty("group_openid") String groupOpenid,
            @JsonProperty("group_name") String groupName,
            @JsonProperty("group_member_num") Integer groupMemberNum
    ) {
    }

    /**
     * 群成员列表响应。
     * <p>Group member list response.</p>
     *
     * @param members    the members
     * @param nextCursor the next page cursor
     */
    public record MemberListData(
            List<Member> members,
            @JsonProperty("next_cursor") String nextCursor
    ) {
    }

    /**
     * 群成员。
     * <p>Group member.</p>
     *
     * @param memberOpenid the member openid
     * @param username     the username
     * @param bot          whether the member is a bot
     * @param memberRole   the member role
     * @param joinedAt     the join time
     * @param unionOpenid  the union openid
     */
    public record Member(
            @JsonProperty("member_openid") String memberOpenid,
            String username,
            Boolean bot,
            @JsonProperty("member_role") String memberRole,
            @JsonProperty("joined_at") String joinedAt,
            @JsonProperty("union_openid") String unionOpenid
    ) {
    }

    /**
     * 机器人资料。
     * <p>Bot profile.</p>
     *
     * @param id       the bot id
     * @param username the bot username
     * @param avatar   the bot avatar url
     */
    public record BotInfo(String id, String username, String avatar) {
    }

    /**
     * 设置群成员禁言请求。
     * <p>Set group member mute request.</p>
     *
     * @param members the mute operations
     */
    public record MuteRequest(List<MuteMember> members) {
    }

    /**
     * 单个群成员禁言操作。
     * <p>One group member mute operation.</p>
     *
     * @param op           add, update, or del
     * @param memberOpenid the member openid
     * @param muteExpireAt the mute expiry in RFC3339
     */
    public record MuteMember(
            String op,
            @JsonProperty("member_openid") String memberOpenid,
            @JsonProperty("mute_expire_at") String muteExpireAt
    ) {
    }
}
