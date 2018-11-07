var userId;
var ws;
var loadingMsg;

$(init);

var Chats = {};

function init(){
    var params = Tparams();
    userId = params.userId;
    if(!userId) return alert('参数未带');
    initChatBox(userId, '自己').a.click();
    Tloader.show(loadingMsg='建立连接中');
    initConn();
}

function initConn(){
    ws = new WebSocket('ws://'+location.host+'/user/ws?userId='+userId);
    ws.onmessage = function(event){
        var rst = Tjson(event.data);
        if(!rst) return console.error(event);
        var data = rst.data;
        switch(data.type){
        case 'connSuc':
            $('#warn').text('连接成功');
            Tloader.hide(loadingMsg);
            loadingMsg = null;
            break;
        case 'msg':
            receive(data.msg);
            break;
        }
    }
    ws.onclose = function(){
        Tloader.change(loadingMsg, loadingMsg='连接断开，重建连接中');
        setTimeout(initConn, 1000);
    }
}

function chatBegin(){
    var to = $('#toId').val();
    if(!to) return;
    initChatBox(to);
    $('#toId').val('');
}

function initChatBox(to, txt){
    if(Chats[to]) return Chats[to];
    if(!txt) txt = to;
    var headId = 'chatHead'+to;
    var boxId = 'chatBox'+to;
    var head = $('<li><a id="'+headId+'" data-toggle="tab" href="#'+boxId+'" aria-expanded="false">'+txt+'</a></li>');
    var box = $(['<div id="'+boxId+'" class="tab-pane fade" style="height:100%;position:relative;">',
         '<pre class="msgs" style="position: absolute;left:0;right:0;top:0;bottom:90px;border-radius:0;"></pre>',
         '<div style="position:absolute;bottom:0;right:0;left:0;height:80px;">',
             '<div style="position:absolute;left:0;right:62px;">',
                 '<textarea class="content" style="resize:none;height:80px;width:100%;"></textarea>',
             '</div>',
             '<button onclick="send(\''+to+'\')" class="btn btn-primary btn-sm" style="right: 0;position:absolute;">发送</button>',
         '</div>',
     '</div>'].join(''));
    $('#chatHeaders').append(head);
    $('#chatBoxs').append(box);
    return Chats[to] = {
        a:head.find('a'),
        msgs:box.find('.msgs'),
        content:box.find('.content'),
    };
}

function send(to){
    var Chat = Chats[to];
    var content = Chat.content.val();
    if(!content) return;
    var chatMsg = {
        from:userId,
        to:to,
        content:content
    };
    ws.send(Tjsf(chatMsg));
    Chat.content.val('');
}

function receive(chatMsg){
    var ele = $('<div>');
    ele.text((chatMsg.from==userId?'':chatMsg.from+'：')+chatMsg.content);
    ele.css('text-align', chatMsg.from==userId?'right':'left');
    var chatBoxId = chatMsg.from;
    if(chatMsg.from==userId) chatBoxId=chatMsg.to;
    initChatBox(chatBoxId).msgs.append(ele);
}