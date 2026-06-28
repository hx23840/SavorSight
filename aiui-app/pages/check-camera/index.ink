<script def>
{
  "navigationBarTitleText": "拍照检查"
}
</script>

<script setup>
import wx from 'wx';

export default {
  data: {
    stepTitle: '',
    targetState: '',
    status: '点击按钮拍照检查',
    btnLabel: '拍照检查',
    btnDisabled: false
  },

  onLoad(query) {
    this.setData({
      stepTitle: decodeURIComponent(query.title || ''),
      targetState: decodeURIComponent(query.targetState || '')
    });
    this.camera = wx.media.createCameraContext();
  },

  takePhoto() {
    if (this.data.btnDisabled) return;
    this.setData({
      status: '正在拍照...',
      btnDisabled: true,
      btnLabel: '拍照中'
    });

    var that = this;
    this.camera.takePhoto({ quality: 'high' }).then(function(photo) {
      that.setData({ status: '正在分析...' });
      setTimeout(function() {
        var results = [
          { status: 'pass', summary: '火候刚好', suggestion: '可以进行下一步', tts: '火候刚好，可以进行下一步了。' },
          { status: 'continue', summary: '还差一点', suggestion: '继续中火翻炒约 20 秒', tts: '还差一点，继续中火翻炒二十秒。' },
          { status: 'adjust', summary: '火有点大', suggestion: '调小火力，避免炒糊', tts: '火有点大，调小火力，避免炒糊。' }
        ];
        var r = results[Math.floor(Math.random() * results.length)];
        var url = '/pages/check-result/index?status=' + encodeURIComponent(r.status)
          + '&summary=' + encodeURIComponent(r.summary)
          + '&suggestion=' + encodeURIComponent(r.suggestion)
          + '&tts=' + encodeURIComponent(r.tts);
        wx.navigateTo({ url: url });
      }, 1500);
    }).catch(function() {
      that.setData({
        status: '拍照失败，请重试',
        btnDisabled: false,
        btnLabel: '重新拍照'
      });
    });
  }
}
</script>

<page>
  <view class="page">
    <view class="head">
      <text class="step-label">{{stepTitle}}</text>
      <text class="target-label">目标状态：{{targetState}}</text>
    </view>

    <view class="status-card">
      <text class="status-text">{{status}}</text>
    </view>

    <view class="action-row">
      <button bindtap="takePhoto">{{btnLabel}}</button>
    </view>
  </view>
</page>

<style>
.page {
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #000000;
  color: #ffffff;
}

.head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.step-label {
  font-size: 18px;
  font-weight: 700;
  color: #40FF5E;
}

.target-label {
  font-size: 12px;
  color: #888888;
}

.status-card {
  flex: 1;
  padding: 20px;
  border-radius: 10px;
  background: #1a1a1a;
  border: 1px solid #333333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  font-size: 15px;
  color: #888888;
  text-align: center;
}

.action-row {
  display: flex;
  gap: 10px;
}

.action-row button {
  flex: 1;
  background: #40FF5E;
  color: #000000;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 700;
}
</style>
