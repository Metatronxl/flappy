
## flappy 服务建立 && 客户端发出请求&&收到响应流程

## 0x01  代理服务端启动流程
###ProxyServerContainer.start()

- 绑定端口，开启server服务端

-> 转入startUserPort()
### ProxyServerContainer.startUserPort()
- `ProxyConfig`的`static块`中配置了`.lanporxy`的详细配置信息
- 从配置表（）中读取配置信息，并监听所有绑定服务的接口

## 0x02 远程客户端启动流程
### ProxyClientContainer()
- 配置bootstrap(与代理服务器进行通信)和realServerBootstrap(与客户端对应的服务端口通信)
### ProxyClientContainer.start()  # connectProxyServer()
- bootstrap连接代理服务器(ip，port),连接成功后发送`ProxyMessage.C_TYPE_AUTH`信息


### 0x03 代理服务器的channel中的ServerChannelHandler 进行`ProxyMessage.C_TYPE_AUTH`逻辑处理
- 获取对应的远程服务器的唯一标识key
- 获取对应的远程服务器所绑定的port列表(由`.lanproxy`配置)
- 获取对应的远程服务器对应的cmdChannel(每个客户端都有一条cmdChannel和多条dataChannel)，如果不存在，则将当前channel（即`ctx.channel()`设置为`cmdChannel`）

### 0x04 代理服务器的channel中的UserChannelHandler.channelActive()
当代理服务器收到请求来自客户端的请求时便会触发UserChannelHandler.channelActive()
- 当前连接的ctx.channel即为userChannel
- 我们通过连接对应的端口来获取对应的cmdChannel
- 设置管道的`ChannlOption.AUTO_READ`为false（因为还没有打通客户端与远程服务器之间的连接）
- 通过cmdChannel发送`ProxyMessage.TYPE_CONNECT`信息

### 0x05 远程服务器的channel中的ClientChannelHandler 进行`ProxyMessage.TYPE_CONNECT`逻辑处理
- 从发来的数据中获取serverInfo（格式为IP:PORT ，ex -> 127.0.0.1:22）
- 连接IP:PORT ,并将对应的realServerChannel的`ChannlOption.AUTO_READ`为false（因为还没有打通代理客户端与远程服务器之间的连接）
- 使用`ClientChannelManager.borrowProxyChannel()`从channel资源池中获取一个空闲的dataChannel，如果没有空闲dataChannel则再次与代理服务器发起连接来新建dataChannel
- dataChannel向代理服务器端发送`ChannelOption.AUTO_READ`，并将realServerChannel与dataChannel互相绑定

> 为什么要采用cmdChannel（控制连接）和ctx.channel()  （真实数据的传输连接）
                         Ans：1.这是因为如果所有的数据都走cmdChannel的话，一旦这是一个传输数据量较大的请求
                         那么channel就会被堵死，在数据没有发送完毕之前无法再互相通信
                              2. 单一的cmdChannel无法建立多用户机制（即无法新建窗口，创立一个新的连接）
                         所以我们采用连接池的方式，一方面，一个新的请求即创建一个新的ctx.channel(真实数据的传输连接)
                         一方面将所有的channel放进连接池中，方便复用且减小创建新的channel的开销
                         
### 0x06 代理服务器的channel中的ServerChannelHandler 进行`ProxyMessage.TYPE_CONNECT`逻辑处理
- 返回的数据中包含了ClientKey，据此可以获得对应的cmdChannel，
- 根据userId获得对应的userChannel
- 将当前channel(ctx.channel())和userChannel进行互相绑定
- 最后设置userChannel可读，这意味着从客户端到远程服务器的连接已经打通

### 0x07 代理服务器的channel中的UserChannelHandler.channelRead0()
在设置完userChannel可读后，channelRead0就可以收到客户端发来的请求信息
- 使用dataChannel发送`ProxyMessage.P_TYPE_TRANSFER`

### 0x08 远程服务器的channel中的ClientChannelHandler 进行`ProxyMessage.P_TYPE_TRANSFER`逻辑处理
- 获取realServerChannel并将数据发送过去

### 0x09 代理服务器的channel中的UserChannelHandler.channelRead0()
因为realServerChannel已经将请求发送给了远程服务器对应的端口，可以在channelRead0()收到发回的响应
- 获取dataChannel(代码中为proxyCHannel),并将收到的响应发回给代理服务器

### 0x10 代理服务器的channel中的ServerChannelHandler 进行`ProxyMessage.P_TYPE_TRANSFER`逻辑处理
- 获取userChannel，并将收到的响应发回给客户端

至此整个代理服务器完成了从建立连接到一次完整的代理服务的请求&&响应流程






