<script def>
{
  "navigationBarTitleText": "做菜导航"
}
</script>

<script setup>
import wx from 'wx';

export default {
  data: {
    stepIdx: 0,
    total: 2,
    stepIndicator: '1 / 2',
    progressWidth: '50%',
    stepTitle: '炒鸡蛋',
    instruction: '鸡蛋液下锅，炒至半凝固后盛出。',
    heat: '中火',
    targetState: '鸡蛋半凝固，表面略湿润',
    prevDisabled: true,
    nextLabel: '下一步',
    firstFocus: true
  },

  onLoad() {
    this.refreshStep();
  },

  refreshStep() {
    var steps = [
      { title: '炒鸡蛋', instruction: '鸡蛋液下锅，炒至半凝固后盛出。', heat: '中火', target: '鸡蛋半凝固，表面略湿润' },
      { title: '炒番茄', instruction: '番茄下锅炒出汁，再加鸡蛋翻炒。', heat: '中火', target: '番茄出汁，锅底有红色汤汁' }
    ];
    var i = this.data.stepIdx;
    var s = steps[i];
    var pct = Math.round(((i + 1) / steps.length) * 100) + '%';
    this.setData({
      stepIndicator: (i + 1) + ' / ' + steps.length,
      total: steps.length,
      progressWidth: pct,
      stepTitle: s.title,
      instruction: s.instruction,
      heat: s.heat,
      targetState: s.target,
      prevDisabled: i === 0,
      nextLabel: i >= steps.length - 1 ? '完成' : '下一步'
    });
  },

  goPrev() {
    if (this.data.prevDisabled) return;
    this.setData({ stepIdx: this.data.stepIdx - 1, firstFocus: false });
    this.refreshStep();
  },

  goNext() {
    if (this.data.stepIdx >= this.data.total - 1) {
      wx.navigateBack();
      return;
    }
    this.setData({ stepIdx: this.data.stepIdx + 1, firstFocus: false });
    this.refreshStep();
  },

  repeatStep() {
    wx.speech.playTTS(this.data.instruction);
  },

  goCheck() {
    var title = encodeURIComponent(this.data.stepTitle);
    var target = encodeURIComponent(this.data.targetState);
    wx.navigateTo({
      url: '/pages/check-camera/index?title=' + title + '&targetState=' + target
    });
  }
}
</script>

<page>
  <view class="page">
    <view class="progress-bar">
      <view class="progress-fill" style="width: {{progressWidth}}"></view>
    </view>

    <view class="step-head">
      <text class="step-indicator">{{stepIndicator}}</text>
      <text class="step-title">{{stepTitle}}</text>
    </view>

    <view class="card">
      <text class="instruction">{{instruction}}</text>
      <view class="meta-row">
        <text class="heat-tag">{{heat}}</text>
        <text class="target">目标：{{targetState}}</text>
      </view>
    </view>

    <view class="btn-row">
      <button class="btn btn-secondary" focus="{{firstFocus}}" bindtap="goPrev">上一步</button>
      <button class="btn btn-secondary" bindtap="repeatStep">重复</button>
      <button class="btn btn-secondary" bindtap="goCheck">检查</button>
      <button class="btn btn-primary" bindtap="goNext">{{nextLabel}}</button>
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

.progress-bar {
  height: 4px;
  background: #1a1a1a;
  border-radius: 2px;
}

.progress-fill {
  height: 100%;
  background: #40FF5E;
  border-radius: 2px;
}

.step-head {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-indicator {
  font-size: 12px;
  color: #888888;
}

.step-title {
  font-size: 20px;
  font-weight: 700;
  color: #40FF5E;
}

.card {
  padding: 12px 14px;
  border-radius: 10px;
  background: #1a1a1a;
  border: 1px solid #333333;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.instruction {
  font-size: 15px;
  line-height: 1.5;
  color: #ffffff;
}

.meta-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.heat-tag {
  font-size: 12px;
  font-weight: 600;
  color: #40FF5E;
  border: 1px solid #40FF5E;
  border-radius: 6px;
  padding: 2px 8px;
}

.target {
  font-size: 12px;
  color: #888888;
  flex: 1;
}

.btn-row {
  display: flex;
  gap: 8px;
}

.btn {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  border-radius: 10px;
  padding: 10px 0;
}

.btn-secondary {
  background: #1a1a1a;
  color: #ffffff;
  border: 1px solid #333333;
}

.btn-primary {
  background: #40FF5E;
  color: #000000;
  border: 1px solid #40FF5E;
}
</style>
