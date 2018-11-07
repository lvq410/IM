$(status);

function status(){
    $.ajax({
        url:'/status.json',
        type:'get',
        dataType:'json',
        success:function(statusMap){
            var trs = [];
          for(var node in statusMap){
              var status = statusMap[node];
              var userRegs = status.userRegs||[];
              var userWss = status.userWss||{};
              var userWssStr = [];
              for(var userId in userWss){
                  userWssStr.push(userId+':'+userWss[userId]);
              }
              trs.push('<tr>',
                  '<td>', node, '</td>',
                  '<td>', Tigh(status.status), '</td>',
                  '<td>', Tigh(userRegs.join('、')), '</td>',
                  '<td>', Tigh(userWssStr.join('; ')), '</td>',
              '</tr>');
              $('#stats').html(trs.join(''));
          }
        },
        error:function(request, msg, obj){
            alert('查询失败');
        }
    });
}