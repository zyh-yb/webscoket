# webscoket

https://blog.csdn.net/qq_45777315/article/details/107681681

## 我的博客，有进阶版，可实现永久指定唯一用户聊天



> 因为我是写的app所以就附带下，后端都是通用的,不是前端人员效果还是有些瑕疵

可以借鉴下，不能只直接用的，后端我改了，先去看看进阶版，在自己研究下吧

 **仅供参考** 

```java
<template>
	<view>
		<!-- 状态栏 -->
		<view class="status_bar"></view>
		<!-- 头部 -->
		<view class="tip">
			{{info.username}}
		</view>
		<!-- 头部 -->
		<view class="box-1" id="list-box" :style="'height:'+contentViewHeight+'px'">
			<scroll-view id="scrollview" scroll-y="true" :scroll-top="scrollTop" @scroll="scroll" style="height: 100%;">
				<view id="msglistview" class="talk-list">
					<view v-for="(item,index) in talkList" :key="index">
						<view class="item flex_col" :class=" item.toUser != uid ? 'push':'pull' ">
							<image class="pic" :src="item.headimage" mode="aspectFill"></image>
							<view class="content" v-if="item.type==1">{{item.message}}</view>
							<view class="content" v-else-if="item.type==3">
								<image @tap="review(item.message)" :src="item.message"></image>
							</view>
						</view>
					</view>
				</view>
			</scroll-view>
		</view>
		<!-- 底部 -->
		<view class="box-2">
			<view class="flex_col"> 
				<image @tap="up" src="../../static/images/more.png" mode="widthFix" class="addimg"></image>
				<view class="flex_grow">
					<input type="text" class="contentbox" v-model="content" placeholder="请输入聊天内容" placeholder-style="color:#DDD;" :cursor-spacing="6">
				</view>
				<button class="send" @tap="send">发送</button>
			</view>
			<view class="imgs" v-if="ishidden">
				<view class="photobox">
					<image @tap="choosePhoto" src="../../static/images/img.png"></image>
					<view class="photoss">相册</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				talkList: [],
				content: '',
				info: {},
				myinfo: {},
				ishidden: false,
				scrollTop: 0,
				old: {
					scrollTop: 0
				},
				contentViewHeight: 0,
				chatstate: false,
				uid: 1,
				timer:null,
				headimage:'' //自己的头像
			}
		},
		onLoad(option) {
			uni.setStorageSync('uid',1);
			let that = this
			//that.uid = option.uid
			//alert(that.uid)
			uni.getSystemInfo({
				success: function(res) { // res - 各种参数
					that.contentViewHeight = res.windowHeight - uni.getSystemInfoSync().screenWidth / 750 * (120);
				}
			});
			// 连接聊天服务器
			this.openSocket()
			this.getHistoryMsg()
			// 获取顶部信息
			this.myUser()
			// 标记已读
			this.$api
				.markRead({  
					// 传给后台的参数
					"fromUid": uni.getStorageSync("uid"),
					"toUid": that.uid
				}, {
					'Content-Type': 'application/json;charset=UTF-8',
					'token': uni.getStorageSync("token")
				})
				.then(res => {
					console.log(res.data)
				})
				.catch(err => {
					// 打印报错信息
					console.log('request fail', err);
				});
		},
		onHide() {
			clearInterval(this.timer)
		},
		onUnload() {
			clearInterval(this.timer)
		},
		methods: {
			// 预览图片
			review(url) {
				let arr = []
				arr.push(url)
				uni.previewImage({
					urls: arr,
					longPressActions: {
						success: function(data) {
							console.log(data);
						},
						fail: function(err) {
							console.log(err.errMsg);
						}
					}
				});
			},
			openSocket() {
				let that = this
				// 连接socket服务器
				uni.connectSocket({
					url: "ws://39.105.86.42:8088/websocket/" + uni.getStorageSync("uid")
				});
				// 检测连接是否成功
				uni.onSocketOpen(function(res) {
					console.log('WebSocket连接已打开！');
					clearInterval(that.timer)
					// that.timer = setInterval(function(){
					// 	uni.sendSocketMessage({
					// 		data: '心跳检测'
					// 	});
					// },5000)
				});
				// 获取服务器信息
				uni.onSocketMessage(function(res) {
					console.log(res)
					let obj = JSON.parse(res.data)
					if (obj.code == 200) {
						that.talkList.push(obj)
					} else {
						console.log(res.data);
					}
					that.$forceUpdate()
					that.scrollToBottom()
					that.chatstate = true
					//}
				});
				// 检测socket连接情况，断线重连
				uni.onSocketError(function(res) {
					console.log('WebSocket连接打开失败，请检查！');
					uni.connectSocket({
						url: "ws://39.105.86.42:8088/websocket/" + uni.getStorageSync("uid")
					});
				});
				// 监测socket是否挂关闭
				uni.onSocketClose(function(res) {
					console.log('WebSocket 已关闭！');
					uni.connectSocket({
						url: "ws://39.105.86.42:8088/websocket/" + uni.getStorageSync("uid")
					});
				});
			},
			// 上传图片
			choosePhoto: function() {
				let that = this
				uni.chooseImage({
					success: (chooseImageRes) => {
						const tempFilePaths = chooseImageRes.tempFilePaths;
						uni.uploadFile({
							url: 'http://39.105.86.42:8088/file/file',
							filePath: tempFilePaths[0],
							name: 'file',
							success: (uploadFileRes) => {
								console.log(uploadFileRes.data);
								let imgurl = uploadFileRes.data.split("\"")[7];
								let obj1 = {
									type: 3,
									types: 1,
									message: imgurl,
									toUser: 2,
								}
								let obj2 = {
									type: 3,
									types: 1,
									message: imgurl,
									toUser: 2,
									headimage: that.headimage
								}
								// 发送聊天信息
								console.log(obj1);
								uni.sendSocketMessage({
									data: JSON.stringify(obj1)
								});
								that.talkList.push(obj2)
								this.$forceUpdate()
								this.scrollToBottom()
								this.chatstate = true
								this.ishidden = false
							}
						})
					}
				})
			},
			// 显示下方工具
			up: function() {
				this.ishidden = !this.ishidden;
			},
			// 获取平台消息
			myUser() {
				let that = this
				uni.request({
				    url: 'http://39.105.86.42:8088/user/user', //仅为示例，并非真实接口地址。
				    data: {
				        uid: 2 //与谁聊天
				    },
					method: 'POST',
				    header: {
				        'Content-Type': 'application/json;charset=UTF-8',
						'token': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhcHAiLCJ1aWQiOjIwMCwiaXNzIjoibGVtb25zb2Z0IiwiZXhwIjoxNTk5NDY2Nzg4LCJpYXQiOjE1OTY4NzQ3ODh9.oYXtGv4h1ZxQFJdvnvk98oNfU7EmsR2qKq3utQ-MmUA'
				    },
				    success: (res) => {
						console.log(res.data.data.data);
						that.info = res.data.data.data
						
				    }
				})
				
				
				//  {
				//      'Content-Type': 'application/json;charset=UTF-8',
				//      'token': uni.getStorageSync("token")
				//     }).then((res) => {
				// 	this.info = res.data.data.data
				// 	uni.setNavigationBarTitle({
				// 		title: this.info.username
				// 	});
				// }).catch((error) => {
				// 	console.log(error)
				// })
				//自己信息
				uni.request({
				    url: 'http://39.105.86.42:8088/user/user', //仅为示例，并非真实接口地址。
				    data: {
				        uid: 1 //自己信息
				    },
					method: 'POST',
				    header: {
				        'Content-Type': 'application/json;charset=UTF-8',
						'token': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhcHAiLCJ1aWQiOjIwMCwiaXNzIjoibGVtb25zb2Z0IiwiZXhwIjoxNTk5NDY2Nzg4LCJpYXQiOjE1OTY4NzQ3ODh9.oYXtGv4h1ZxQFJdvnvk98oNfU7EmsR2qKq3utQ-MmUA'
				    },
				    success: (res) => {
						console.log(res.data.data.data);
						this.headimage=res.data.data.data.headimage
				    }
				})
				
				// this.$api.myUser({
				// 	// 传给后台的参数
				// 	"uid": uni.getStorageSync("uid")
				// },
				//  {
				//      'Content-Type': 'application/json;charset=UTF-8',
				//      'token': uni.getStorageSync("token")
				//     }).then((res) => {
				// 	this.myinfo = res.data.data.data
				// }).catch((error) => {
				// 	console.log(error)
				// })
			},
			// 获取历史消息
			getHistoryMsg() {
				let that = this
				uni.request({ 
				    url: 'http://39.105.86.42:8088/user/selChatting', //仅为示例，并非真实接口地址。
				    data: {
				        fid: 1,
						tid: 2
				    },
					method: 'POST',
				    header: {
				        'Content-Type': 'application/json;charset=UTF-8',
						'token': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhcHAiLCJ1aWQiOjIwMCwiaXNzIjoibGVtb25zb2Z0IiwiZXhwIjoxNTk5NDY2Nzg4LCJpYXQiOjE1OTY4NzQ3ODh9.oYXtGv4h1ZxQFJdvnvk98oNfU7EmsR2qKq3utQ-MmUA'
				    },
				    success: (res) => {
				        console.log(res.data.data.data);
				        let arr = res.data.data.data
						for(let index in arr){
							let obj = arr[index]
							that.talkList.push(obj)
						}
						this.$forceUpdate()
						this.scrollToBottom()
						this.chatstate = true
						
				    }
				})
				//  {
				//      'Content-Type': 'application/json;charset=UTF-8',
				//      'token': uni.getStorageSync("token")
				//     }).then((res) => {
				// 	let arr = res.data.data.list
				// 	for(let index in arr){
				// 		let obj = arr[index]
				// 		if(obj.type!=3){
				// 			that.talkList.push(obj)
				// 		}else{
				// 			that.articleid = obj.articleId
				// 			this.getname(obj.articleId)
				// 			console.log("文章ID",obj.articleId)
				// 		}
				// 	}
				// 	this.$forceUpdate()
				// 	this.scrollToBottom()
				// 	this.chatstate = true
				// }).catch((error) => {
				// 	console.log(error)
				// })
			},
			// 滚动到底部
			scrollToBottom(t) {
				let that = this
				uni.getSystemInfo({
					success: function(res) { // res - 各种参数
						let query = uni.createSelectorQuery()
						query.select('#scrollview').boundingClientRect()
						query.select('#msglistview').boundingClientRect()
						query.exec((res) => {
							let mainheight = res[1].height
							that.old.scrollTop = mainheight
							that.scrollTop = mainheight
							that.gobottom()
						})
					}
				});
			},
			gobottom(t) {
				this.$nextTick(function() {
					this.scrollTop = this.old.scrollTop + 520
				});
				this.scrollTop = 0
			},
			scroll(e) {
				let that = this
				if (that.chatstate == true) {
					this.old.scrollTop = e.detail.scrollTop
					that.chatstate = false
				} else {

				}
			},
			// 发送文本信息
			send() {
				if (!this.content) {
					uni.showToast({
						title: '请输入有效的内容',
						icon: 'none'
					})
					return;
				}
				// 发送聊天信息
				let obj1 = {
					type: 1,
					types:1,
					message:this.content,
					toUser:2  ,//发给谁
				}
				let obj2 = {
					type: 1,
					types:1,
					message:this.content,
					toUser:2  ,//发给谁
					headimage: this.headimage
				}
				uni.sendSocketMessage({
					data: JSON.stringify(obj1)
				});
				// 插入自己的聊天记录
				this.talkList.push(obj2)
				// 置空输入框
				this.content = '';
				this.$forceUpdate()
				this.scrollToBottom()
				this.chatstate = true
			}
		}
	}
</script>

<style lang="scss">
@import "../../lib/global.scss";
page {
	background-color: #F3F3F3;
	font-size: 28rpx;
}
//状态栏
.status_bar {
	height: var(--status-bar-height);
	width: 100%;
	background-color: rgba(255, 255, 255, 1);
}
// 头部
.tip{
	position: fixed;
	top: var(--status-bar-height);
	right: 0;
	z-index: 100000;
	background-color: #FFFFFF;
	width: 100%;
	height: 100rpx;
	line-height: 100rpx;
	text-align: center;
}
/* 加载数据提示 */
.tips {
	position: fixed;
	left: 0;
	top:var(--window-top);
	width: 100%;
	z-index: 9;
	background-color: rgba(0, 0, 0, 0.15);
	height: 72rpx;
	line-height: 72rpx;
	transform: translateY(-80rpx);
	transition: transform 0.3s ease-in-out 0s;

	&.show {
		transform: translateY(0);
	}
}
/*中间部分*/
.box-1 {
	width: 100%;
	// min-height: 100%;
	padding-bottom: 100rpx;
	box-sizing: content-box;
	/* 兼容iPhoneX */
	margin-bottom: 0;
	margin-bottom: constant(safe-area-inset-bottom);
	margin-bottom: env(safe-area-inset-bottom);
}
.talk-list {
	/* 消息项，基础类 */
	.item {
		padding: 20rpx 20rpx 0 20rpx;
		align-items: flex-start;
		align-content: flex-start;
		color: #333;
		/*头像*/
		.pic {
			width: 92rpx;
			height: 92rpx;
			border-radius: 50%;
			border: #fff solid 1px;
		}
		/*消息内容*/
		.content {
			padding: 20rpx;
			border-radius: 4px;
			max-width: 500rpx;
			word-break: break-all;
			line-height: 52rpx;
			position: relative;
		}
		/* 收到的消息 */
		&.pull {
			.content {
				margin-left: 32rpx;
				background-color: #fff;
				&::after {
					content: '';
					display: block;
					width: 0;
					height: 0;
					border-top: 16rpx solid transparent;
					border-bottom: 16rpx solid transparent;
					border-right: 20rpx solid #fff;
					position: absolute;
					top: 30rpx;
					left: -18rpx;
				}
			}
		}

		/* 发出的消息 */
		&.push {
			/* 主轴为水平方向，起点在右端。使不修改DOM结构，也能改变元素排列顺序 */
			flex-direction: row-reverse;
			.content {
				margin-right: 32rpx;
				background-color: #a0e959;
				&::after {
					content: '';
					display: block;
					width: 0;
					height: 0;
					border-top: 16rpx solid transparent;
					border-bottom: 16rpx solid transparent;
					border-left: 20rpx solid #a0e959;
					position: absolute;
					top: 30rpx;
					right: -18rpx;
				}
			}
		}
	}
}
/*图片消息*/
.content image {
	width: 164rpx;
	height: 164rpx;
}
/*中间部分结束*/
/*底部start*/
.box-2 {
	position: fixed;
	left: 0;
	width: 100%;
	bottom: 0;
	height: auto;
	z-index: 2;
	border-top: #e5e5e5 solid 1px;
	box-sizing: content-box;
	background-color: #F3F3F3;
	/* 兼容iPhoneX */
	padding-bottom: 0;
	padding-bottom: constant(safe-area-inset-bottom);
	padding-bottom: env(safe-area-inset-bottom);
	>view {
		padding: 0 20rpx;
		height: 100rpx;
	}
	/*输入聊天内容*/
	.contentbox {
		background-color: #fff;
		height: 64rpx;
		line-height: 30rpx;
		padding: 0 20rpx;
		border-radius: 32rpx;
		font-size: 28rpx;
	}
	/*发送*/
	.send {
		background-color: #42b983;
		color: #fff;
		height: 64rpx;
		margin-left: 20rpx;
		border-radius: 32rpx;
		padding: 0;
		width: 120rpx;
		line-height: 62rpx;
		&:active {
			background-color: #5fc496;
		}
	}
}
/*选择图片*/
.imgs {
	width: 750rpx;
	min-height: 220rpx;
}
.imgs image {
	width: 90rpx;
	height: 90rpx;
}
.photobox {
	width: 130rpx;
	height: 130rpx;
	display: flex;
	justify-content: space-between;
	align-items: center;
	flex-direction: column;
}
/*相册*/
.photoss {
	height: 30rpx;
	color: rgba(80, 80, 80, 1);
	font-size: 28rpx;
	line-height: 30rpx;
	text-align: center;
	text-align: center;
}
/*底部左侧图标*/
.addimg {
	width: 48rpx;
	height: 48rpx;
	margin-right: 5rpx;
}
/*底部end*/

/*
.user {
	position: fixed;
	top: 0;
	width: 750rpx;
	height: 102rpx;
	padding-top: var(--status-bar-height+50px);
	display: flex;
	align-items: center;
	border-bottom: 10rpx solid #eee;
	z-index: 10000;
}
.headPhoto,
.userInfo {
	padding: 12rpx 0 12rpx 20rpx;
	font-size: 24rpx;
}
.fcolor {
	color: rgba(128, 128, 128, 1);
	font-size: 20rpx;
}
.headPhoto image {
	width: 78rpx;
	height: 78rpx;
	border-radius: 40rpx;
}*/
</style>

```

