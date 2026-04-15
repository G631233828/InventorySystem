const DOM = {
  signForm: null,
  locateBtn: null,
  signAddress: null,
  locationStatus: null,
  iosWechatTip: null,
  helpBtn: null,
  helpModal: null,
  closeHelpBtn: null,
  successToast: null,
  submitBtn: null,
  photosInput: null,
  previewContainer: null,
  photoCount: null,
  userName: null,
  locationText: null,
  showAddress: null,
  schoolSearch: null,
  schoolDropdown: null,
  schoolId: null
};

const appState = {
  isiOSWechat: false,
  uploadedFiles: [], // 压缩后的文件
  isLocating: false,
  BMap: null,
  geocoder: null
};

function loadBaiduMap() {
  return new Promise((resolve, reject) => {
    if (window.BMap) {
      appState.BMap = window.BMap;
      appState.geocoder = new BMap.Geocoder();
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://api.map.baidu.com/api?v=2.0&ak=O1etBmdvVAYUTTR3S3nZz2l0S4br8v4p&callback=baiduMapReady';
    window.baiduMapReady = function () {
      appState.BMap = window.BMap;
      appState.geocoder = new BMap.Geocoder();
      resolve();
    };
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

const CoordConvert = (function () {
  const x_pi = 3.14159265358979324 * 3000.0 / 180.0;
  const pi = 3.1415926535897932384626;
  const a = 6378245.0;
  const ee = 0.00669342162296594323;

  function outOfChina(lat, lon) {
    return (lon < 72.004 || lon > 137.8347 || lat < 0.8293 || lat > 55.8271);
  }
  function transformLat(x, y) {
    let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
    ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;
    ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;
    return ret;
  }
  function transformLon(x, y) {
    let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
    ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;
    ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;
    ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;
    return ret;
  }
  function wgs84ToGcj02(lat, lon) {
    if (outOfChina(lat, lon)) return [lat, lon];
    let dLat = transformLat(lon - 105.0, lat - 35.0);
    let dLon = transformLon(lon - 105.0, lat - 35.0);
    let radLat = lat / 180.0 * pi;
    let magic = Math.sin(radLat);
    magic = 1 - ee * magic * magic;
    let sqrtMagic = Math.sqrt(magic);
    dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
    dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
    return [lat + dLat, lon + dLon];
  }
  function gcj02ToBd09(lat, lon) {
    let z = Math.sqrt(lon * lon + lat * lat) + 0.00002 * Math.sin(lat * x_pi);
    let theta = Math.atan2(lat, lon) + 0.000003 * Math.cos(lon * x_pi);
    let bdLon = z * Math.cos(theta) + 0.0065;
    let bdLat = z * Math.sin(theta) + 0.006;
    return [bdLat, bdLon];
  }
  function wgs84ToBd09(lat, lon) {
    let gcj = wgs84ToGcj02(lat, lon);
    return gcj02ToBd09(gcj[0], gcj[1]);
  }
  return { wgs84ToBd09 };
})();

function initDOMReferences() {
  DOM.signForm = document.getElementById('signForm');
  DOM.locateBtn = document.getElementById('locateBtn');
  DOM.signAddress = document.getElementById('signAddress');
  DOM.locationStatus = document.getElementById('locationStatus');
  DOM.iosWechatTip = document.getElementById('iosWechatTip');
  DOM.helpBtn = document.getElementById('helpBtn');
  DOM.helpModal = document.getElementById('helpModal');
  DOM.closeHelpBtn = document.getElementById('closeHelpBtn');
  DOM.successToast = document.getElementById('successToast');
  DOM.submitBtn = document.getElementById('submitBtn');
  DOM.photosInput = document.getElementById('photosInput');
  DOM.previewContainer = document.getElementById('previewContainer');
  DOM.photoCount = document.getElementById('photoCount');
  DOM.userName = document.getElementById('userName');
  DOM.locationText = document.getElementById('locationText');
  DOM.showAddress = document.getElementById('showAddress');
  DOM.schoolSearch = document.getElementById('schoolSearch');
  DOM.schoolDropdown = document.getElementById('schoolDropdown');
  DOM.schoolId = document.getElementById('schoolId');
}

document.addEventListener('DOMContentLoaded', () => {
  initDOMReferences();
  initEnvCheck();
  initEvent();
  initSchoolSearch();
  initPhotoUpload();
});

function initEnvCheck() {
  const ua = navigator.userAgent.toLowerCase();
  appState.isiOSWechat = /iphone|ipad|ipod/.test(ua) && /micromessenger/.test(ua);
  if (appState.isiOSWechat) {
    DOM.iosWechatTip.classList.remove('hidden');
  }
}

function initEvent() {
  DOM.locateBtn.addEventListener('click', startLocation);
  DOM.helpBtn.addEventListener('click', () => {
    DOM.helpModal.classList.remove('hidden');
    DOM.helpModal.classList.add('flex');
  });
  DOM.closeHelpBtn.addEventListener('click', () => {
    DOM.helpModal.classList.add('hidden');
    DOM.helpModal.classList.remove('flex');
  });
  DOM.signForm.addEventListener('submit', handleSubmit);
}

function initSchoolSearch() {
  let timer = null;
  DOM.schoolSearch.addEventListener('input', function () {
    clearTimeout(timer);
    const val = this.value.trim();
    DOM.schoolId.value = '';

    if (val.length < 1) {
      DOM.schoolDropdown.classList.add('hidden');
      DOM.schoolDropdown.innerHTML = '';
      return;
    }

    timer = setTimeout(async () => {
      try {
        const res = await fetch('../wechatrp/searchSchool?keyword=' + encodeURIComponent(val));
        const data = await res.json();
        DOM.schoolDropdown.innerHTML = '';

        if (data.status === 200 && data.data && data.data.length > 0) {
          data.data.forEach(school => {
            const item = document.createElement('div');
            item.className = 'px-4 py-2 hover:bg-gray-100 cursor-pointer text-sm';
            item.textContent = school.schoolName;
            item.onclick = () => {
              DOM.schoolSearch.value = school.schoolName;
              DOM.schoolId.value = school.id;
              DOM.schoolDropdown.classList.add('hidden');
            };
            DOM.schoolDropdown.appendChild(item);
          });
        } else {
          DOM.schoolDropdown.innerHTML = '<div class="px-4 py-2 text-gray-400 text-sm">未找到匹配学校</div>';
        }
        DOM.schoolDropdown.classList.remove('hidden');
      } catch (e) {
        console.error(e);
      }
    }, 250);
  });

  document.addEventListener('click', (e) => {
    if (!DOM.schoolSearch.contains(e.target) && !DOM.schoolDropdown.contains(e.target)) {
      DOM.schoolDropdown.classList.add('hidden');
    }
  });
}

async function startLocation() {
  if (appState.isLocating) return;
  if (!navigator.geolocation) {
    showStatus("设备不支持定位", "error");
    return;
  }

  appState.isLocating = true;
  DOM.locateBtn.disabled = true;
  DOM.locateBtn.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i>定位中...';
  showStatus("正在获取精准位置...", "info");

  try {
    await loadBaiduMap();
  } catch (e) {
    showStatus("地图加载失败", "error");
    resetBtn();
    return;
  }

  const opt = { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 };
  let retry = 0;

  function get() {
    navigator.geolocation.getCurrentPosition(async pos => {
      const lat = pos.coords.latitude;
      const lng = pos.coords.longitude;
      const [bdLat, bdLng] = CoordConvert.wgs84ToBd09(lat, lng);

      const point = new BMap.Point(bdLng, bdLat);
      appState.geocoder.getLocation(point, res => {
        if (res && res.address) {
          let addr = res.address;
          const poi = res.surroundingPois?.[0]?.title || '';
          if (poi) addr += `（${poi}）`;
          DOM.signAddress.value = addr;
          DOM.showAddress.textContent = addr;
          DOM.locationText.classList.remove('hidden');
          showStatus(`定位成功：${addr}`, "success");
        } else {
          const def = `${bdLat.toFixed(6)}, ${bdLng.toFixed(6)}`;
          DOM.signAddress.value = def;
          DOM.showAddress.textContent = def;
          DOM.locationText.classList.remove('hidden');
          showStatus("已获取坐标位置", "success");
        }
        resetBtn();
      });
    }, err => {
      retry++;
      if (retry <= 2) {
        showStatus(`定位失败，重试 ${retry}/2`, "info");
        setTimeout(get, 1000);
        return;
      }
      showStatus("定位失败，请检查权限", "error");
      resetBtn();
    }, opt);
  }
  get();
}

function resetBtn() {
  appState.isLocating = false;
  DOM.locateBtn.disabled = false;
  DOM.locateBtn.innerHTML = '<i class="fa fa-map-marker mr-2"></i>点击获取精准位置';
}

function showStatus(msg, type) {
  DOM.locationStatus.classList.remove('hidden');
  DOM.locationStatus.className = type === "error" ? "text-xs text-danger mt-1" : "text-xs text-neutral mt-1";
  DOM.locationStatus.innerHTML = `<i class="fa fa-info-circle mr-1"></i>${msg}`;
}

// ====================== 图片压缩 + 上传 ======================
function initPhotoUpload() {
  DOM.photosInput.addEventListener('change', handleFileSelect);
}

async function compressImage(file) {
  return new Promise((resolve) => {
    const img = new Image();
    img.src = URL.createObjectURL(file);
    img.onload = function () {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      let width = img.width;
      let height = img.height;
      const maxSize = 1280;

      if (width > maxSize) {
        height = (maxSize / width) * height;
        width = maxSize;
      }
      if (height > maxSize) {
        width = (maxSize / height) * width;
        height = maxSize;
      }

      canvas.width = width;
      canvas.height = height;
      ctx.drawImage(img, 0, 0, width, height);

      canvas.toBlob(blob => {
        const compressedFile = new File([blob], `compressed_${file.name}`, { type: 'image/jpeg' });
        resolve(compressedFile);
      }, 'image/jpeg', 0.7);
    };
  });
}

async function handleFileSelect(e) {
  const files = Array.from(e.target.files);
  if (files.length === 0) return;

  const total = appState.uploadedFiles.length + files.length;
  if (total > 5) {
    alert("最多上传5张");
    e.target.value = "";
    return;
  }

  DOM.previewContainer.classList.remove('hidden');
  DOM.photoCount.classList.remove('hidden');

  for (const file of files) {
    if (!file.type.startsWith('image/')) {
      alert("仅支持图片");
      continue;
    }
    try {
      const compressed = await compressImage(file);
      appState.uploadedFiles.push(compressed);

      const div = document.createElement('div');
      div.className = "relative bg-gray-100 rounded-lg overflow-hidden aspect-square";
      const img = document.createElement('img');
      img.src = URL.createObjectURL(compressed);
      img.className = "w-full h-full object-cover";
      div.appendChild(img);
      DOM.previewContainer.appendChild(div);
    } catch (err) {
      console.error(err);
    }
  }

  DOM.photoCount.textContent = `已选择 ${appState.uploadedFiles.length}/5 张（已压缩）`;
  e.target.value = "";
}
// ============================================================

async function handleSubmit(e) {
  e.preventDefault();

  const name = DOM.userName.value?.trim();
  const schoolId = DOM.schoolId.value?.trim();
  const addr = DOM.signAddress.value?.trim();
  const classRoom = document.getElementById("classRoom").value.trim();

  if (!name) { alert("请输入姓名"); return; }
  if (!schoolId) { alert("请选择签到学校"); return; }
  if (!classRoom) { alert("请选择负责教室"); return; }
  if (!addr) { alert("请先获取定位"); return; }
  if (appState.uploadedFiles.length === 0) { alert("请上传现场照片"); return; }

  DOM.submitBtn.disabled = true;
  DOM.submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i>提交中...';

  const fd = new FormData();
  fd.append("openId", document.getElementById("openId").value);
  fd.append("name", name);
  fd.append("schoolId", schoolId);
  fd.append("address", addr+","+classRoom);
  fd.append("problems", "");
  appState.uploadedFiles.forEach(f => fd.append("photos", f));

  try {
    const res = await fetch("../wechatrp/sign", {
      method: "POST",
      body: fd,
      credentials: "include"
    });
    const data = await res.json();
    if (data.status === 200) {
      alert("签到成功！");
      location.reload();
    } else {
      alert("失败：" + (data.msg || "未知错误"));
    }
  } catch (err) {
    console.error(err);
    alert("网络异常");
  } finally {
    DOM.submitBtn.disabled = false;
    DOM.submitBtn.innerHTML = '<i class="fa fa-paper-plane mr-2"></i>确认签到';
  }
}