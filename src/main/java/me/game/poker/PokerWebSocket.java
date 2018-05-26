package me.game.poker;

import com.google.gson.Gson;
import me.game.poker.entity.Player;
import me.game.poker.entity.Room;
import me.game.poker.manager.RoomManager;
import me.game.poker.utils.SocketRequest;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 * 使用springboot的唯一区别是要@Component声明下，而使用独立容器是由容器自己管理websocket的，但在springboot中连容器都是spring管理的。
 * 虽然@Component默认是单例模式的，但springboot还是会为每个websocket连接初始化一个bean，所以可以用一个静态set保存起来。
 */
@ServerEndpoint("/pokerWebSocket/{userId}")
@Component
public class PokerWebSocket {
    /**
     * 存活的session集合（使用线程安全的map保存）
     */
    private static Map<String, Session> livingSessions = new ConcurrentHashMap<>();

    private String response(Integer code,Object data){
        Gson gson = new Gson();
        SocketResult result = new SocketResult();
        result.setCode(code);
        result.setData(data);
        return gson.toJson(result);
    }
    /**
     * 建立连接的回调方法
     * @param session 与客户端的WebSocket连接会话
     */
    @OnOpen
    public void onOpen(Session session,@PathParam("userId") String userId) {
        livingSessions.put(session.getId(), session);
        System.out.println(userId+  " 连接成功");
    }

    /**
     * 收到客户端消息的回调方法
     * @param message 客户端传过来的消息
     * @param session 对应的session
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") String userId) {
        System.out.println(userId + " : " + message);
        Gson gson = new Gson();
        SocketRequest request = gson.fromJson(message,SocketRequest.class);
        System.out.println("request:" + request);
        if(request.getCode() == RoomManager.Request_BeReady){

        }
        switch (request.getCode()){
            case RoomManager.Request_IntoRoom:{
                joinTheRoom(session,request.getData());
            } break;
            /*
             *准备完毕
             */
            case RoomManager.Request_BeReady: {

            } break;
            /*
             *叫地主
             */
            case RoomManager.Request_CallTheLandlord:{

            } break;
            default:break;
        }
    }
    /**
     *
     * 发生错误的回调方法
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     *  关闭连接的回调方法
     */
    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        livingSessions.remove(session.getId());
        sendMessageToAll(userId + " 退出聊天室");
    }
    /**
     * 单独发送消息
     * @param session
     * @param message
     */
    public void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 群发消息
     * @param message
     */
    public void sendMessageToAll(String message) {
        livingSessions.forEach((sessionId, session) -> {
            sendMessage(session, message);
        });
    }

    public void joinTheRoom(Session session ,Map<String,Object> userDate){
        try {
            String userId = userDate.get("userId").toString();
            String userName = userDate.get("userName").toString();

            Player player = new Player(userId,userName);
            synchronized(RoomManager.room){
                if(RoomManager.room == null) {
                    RoomManager.room = new Room();
                }
                Room room = RoomManager.room;
                player.setRoomId(room.getId());
                int seat = room.getSeat();
                player.setSeat(seat);
                /*
                    抢到位置，将玩家加入房间
                */
                if(seat > 0){
                    room.getPlayers().add(player);
                    Map<String,Object> map = new HashMap<>();
                    map.put("roomId",room.getId());
                    map.put("seat",seat);
                    map.put("players",room.getPlayers());
                    //进入房间成功，向客户端发送当前房间信息
                    sendMessage(session,response(RoomManager.Response_RoomInfo,map));
                }

                /*
                    判断房间是否已满
                */
                if(seat == 3){
                    RoomManager.roomMap.put(room.getId(),room);
                    RoomManager.room = new Room();
                }
            }//synchronized
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}
