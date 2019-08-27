# flappy

一个玩具级别的内网穿透服务

## 0x01 主要功能
flappy是一个将局域网个人电脑、服务器代理到公网的内网穿透工具，目前仅支持tcp流量转发，可支持任何tcp上层协议（访问内网网站、本地支付接口调试、ssh访问、远程桌面...）


## 0x02  配置 

### server配置

该配置为代理服务器的配置，代理服务器端需要配置详细的代理信息，具体的配置格式示例如下
```
[
  {
    "name": "home_linux",
    "clientKey": "6b1c4bfbd674423d84915390dfc5c173",
    "proxyMappings": [
      {
        "name": "lab_linux",
        "inetPort": "2223",
        "lan": "127.0.0.1:22"
      }
    ],
    "status": 0
  },
  {
    "name": "mac_test",
    "clientKey": "a1c626e56a4e44bbb4bc5f99549b2f55",
    "proxyMappings": [
      {
        "inetPort": 23334,
        "lan": "127.0.0.1:80",
        "name": "mac_test_proxy"
      },
      {
        "inetPort": 23335,
        "lan": "127.0.0.1:22",
        "name": "mac_test_ssh"
      }
    ],
    "status": 0
  }
]

```
其中
- `lan`是指需要穿透的内网客户端的接口
- `inetPort`是指在代理客户端上的接口
- `name`是指服务代称
- `status`是指目前是否活跃（暂未启用）

server的配置文件放置在conf目录中，配置 config.properties
```
server.bind=0.0.0.0
 
 #与代理客户端通信端口
 server.port=4900
 
 #ssl相关配置
 server.ssl.enable=true
 server.ssl.bind=0.0.0.0
 server.ssl.port=4993
 server.ssl.jksPath=test.jks
 server.ssl.keyStorePassword=123456
 server.ssl.keyManagerPassword=123456
 
 #这个配置可以忽略
 server.ssl.needsClientAuth=false


```


### client配置

```
#与在proxy-server配置后台创建客户端时填写的秘钥保持一致；
client.key=
ssl.enable=true
ssl.jksPath=test.jks
ssl.keyStorePassword=123456

#这里填写实际的proxy-server地址；没有服务器默认即可，自己有服务器的更换为自己的proxy-server（IP）地址
server.host=lp.thingsglobal.org

#proxy-server ssl默认端口4993，默认普通端口4900
#ssl.enable=true时这里填写ssl端口，ssl.enable=false时这里填写普通端口
server.port=4993

```

## 0x03 服务启动

- 代理服务器端使用`server_restart.sh`&& `server_stop.sh`
- 内网客户端使用`client_restart.sh`&&`client_stop.sh`

## 0x04 使用技术

- Netty
— SpringBoot

## 0x05 环境要求

- 安装java1.7或以上环境
- Maven

## 0x06 关于内网穿透的核心流程

请参考[document.md](https://github.com/Metatronxl/flappy/blob/master/document.md)



# TODO

- 配置信息使用web修改（配置信息暂时放置在 /.flappy/config.json）
- 配置热更新时的处理逻辑


##  致谢

ffay大大的[lanproxy](https://github.com/ffay/lanproxy)
