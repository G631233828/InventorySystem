// 优化6：使用常量缓存DOM查询结果，减少重复DOM查找
const DOM = {
  repairForm: null,
  locateBtn: null,
  schoolAddress: null,
  locationStatus: null,
  iosWechatTip: null,
  faultInformation: null,
  charCount: null,
  previewContainer: null,
  helpBtn: null,
  helpModal: null,
  closeHelpBtn: null,
  successToast: null,
  mapContainer: null,
  confirmLocationBtn: null,
  bracketedAddressText: null,
  contactNumberInput: null,
  contactNumberError: null,
  photoCount: null,
  equipmentYearInput: null,
  expectedVisitTimeInput: null,
  submitBtn: null,
  photosInput: null,
  // 新增：设备相关DOM
  equipmentRepair: null,
  equipmentDropdownBtn: null,
  equipmentDropdown: null,
  // 新增：故障详情模态框相关DOM
  faultDetailModal: null,
  closeFaultModalBtn: null,
  faultDetailList: null,
  confirmFaultDetailBtn: null
};

// 全局状态管理（优化7：集中管理状态，减少全局变量）
const appState = {
  map: null,
  marker: null,
  BMap: null,
  geocoder: null,
  isiOSWechat: false,
  mapLoaded: false,
  uploadedFiles: [],
  isMapLoading: false,
  isSamsungDevice: false,
  mapScriptLoaded: false, // 标记地图脚本是否已加载
  // 新增：设备和故障相关状态
  deviceNames: [], // 设备名称列表
  faultDetails: [], // 故障详情列表
  selectedFaultDetail: '' // 选中的故障详情
};

// 优化8：防抖函数，减少高频事件触发
function debounce(func, wait = 300) {
  let timeout;
  return function(...args) {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(this, args), wait);
  };
}

// 初始化DOM引用（优化9：一次性查询所有DOM，减少重排）
function initDOMReferences() {
  DOM.repairForm = document.getElementById('repairForm');
  DOM.locateBtn = document.getElementById('locateBtn');
  DOM.schoolAddress = document.getElementById('schoolAddress');
  DOM.locationStatus = document.getElementById('locationStatus');
  DOM.iosWechatTip = document.getElementById('iosWechatTip');
  DOM.faultInformation = document.getElementById('faultInformation');
  DOM.charCount = document.getElementById('charCount');
  DOM.previewContainer = document.getElementById('previewContainer');
  DOM.helpBtn = document.getElementById('helpBtn');
  DOM.helpModal = document.getElementById('helpModal');
  DOM.closeHelpBtn = document.getElementById('closeHelpBtn');
  DOM.successToast = document.getElementById('successToast');
  DOM.mapContainer = document.getElementById('mapContainer');
  DOM.confirmLocationBtn = document.getElementById('confirmLocationBtn');
  DOM.bracketedAddressText = document.getElementById('bracketedAddressText');
  DOM.contactNumberInput = document.getElementById('contactNumber');
  DOM.contactNumberError = document.getElementById('contactNumberError');
  DOM.photoCount = document.getElementById('photoCount');
  DOM.equipmentYearInput = document.getElementById('equipmentYear');
  DOM.expectedVisitTimeInput = document.getElementById('expectedVisitTime');
  DOM.submitBtn = document.getElementById('submitBtn');
  DOM.photosInput = document.getElementById('photosInput');
  // 新增：设备相关DOM初始化
  DOM.equipmentRepair = document.getElementById('equipmentRepair');
  DOM.equipmentDropdownBtn = document.getElementById('equipmentDropdownBtn');
  DOM.equipmentDropdown = document.getElementById('equipmentDropdown');
  // 新增：故障详情模态框DOM初始化
  DOM.faultDetailModal = document.getElementById('faultDetailModal');
  DOM.closeFaultModalBtn = document.getElementById('closeFaultModalBtn');
  DOM.faultDetailList = document.getElementById('faultDetailList');
  DOM.confirmFaultDetailBtn = document.getElementById('confirmFaultDetailBtn');
}

// 页面初始化（优化10：分阶段初始化，优先初始化关键功能）
document.addEventListener('DOMContentLoaded', () => {
  console.log('页面初始化开始');
  // 1. 优先初始化DOM引用
  initDOMReferences();
  
  // 2. 初始化关键设备检测
  initDeviceDetection();
  
  // 3. 初始化核心表单功能（首屏必需）
  initFormBasic();
  
  // 4. 初始化设备下拉功能
  initEquipmentDropdown();
  
  // 5. 初始化故障详情模态框
  initFaultDetailModal();
  
  // 6. 延迟初始化非关键功能（优化11：空闲时初始化）
  if ('requestIdleCallback' in window) {
    requestIdleCallback(() => {
      initEnvCheck();
      checkLocationPermission();
    }, { timeout: 2000 });
  } else {
    setTimeout(() => {
      initEnvCheck();
      checkLocationPermission();
    }, 500);
  }
  
  // 7. 绑定核心事件
  DOM.locateBtn.addEventListener('click', handleLocateClick);
  console.log('页面初始化完成');
});

/**
 * 新增：初始化设备下拉功能
 */
function initEquipmentDropdown() {
  // 绑定下拉按钮点击事件
  DOM.equipmentDropdownBtn.addEventListener('click', async () => {
    // 切换下拉列表显示状态
    if (DOM.equipmentDropdown.classList.contains('hidden')) {
      // 加载设备名称列表
      await loadDeviceNames();
      DOM.equipmentDropdown.classList.remove('hidden');
    } else {
      DOM.equipmentDropdown.classList.add('hidden');
    }
  });

  // 点击页面其他区域关闭下拉列表
  document.addEventListener('click', (e) => {
    if (!e.target.closest('#equipmentRepair').parentElement) {
      DOM.equipmentDropdown.classList.add('hidden');
    }
  });
}

/**
 * 新增：加载设备名称列表
 */
async function loadDeviceNames() {
  try {
    const response = await fetch('../wechatrp/getAllDeviceNames', {
      method: 'GET',
      credentials: 'include'
    });
    
    const result = await response.json();
    
    if (result.status === 200 && result.data && Array.isArray(result.data)) {
      appState.deviceNames = result.data;
      // 渲染设备列表
      renderDeviceDropdown();
    } else {
      alert(`获取设备名称失败：${result.msg || '未知错误'}`);
    }
  } catch (error) {
    console.error('加载设备名称失败:', error);
    alert('获取设备名称失败，请稍后重试');
  }
}

/**
 * 新增：渲染设备下拉列表
 */
function renderDeviceDropdown() {
  DOM.equipmentDropdown.innerHTML = '';
  
  if (appState.deviceNames.length === 0) {
    const emptyItem = document.createElement('div');
    emptyItem.className = 'device-dropdown-item text-neutral';
    emptyItem.textContent = '暂无设备数据';
    DOM.equipmentDropdown.appendChild(emptyItem);
    return;
  }
  
  appState.deviceNames.forEach(deviceName => {
    const item = document.createElement('div');
    item.className = 'device-dropdown-item';
    item.textContent = deviceName;
    item.addEventListener('click', async () => {
      // 将选中的设备名称填入输入框
      DOM.equipmentRepair.value = deviceName;
      // 关闭下拉列表
      DOM.equipmentDropdown.classList.add('hidden');
      // 加载该设备的故障详情
      await loadFaultDetails(deviceName);
      // 显示故障详情模态框
      showFaultDetailModal();
    });
    DOM.equipmentDropdown.appendChild(item);
  });
}

/**
 * 新增：加载故障详情
 */
async function loadFaultDetails(deviceName) {
  try {
    const params = new URLSearchParams();
    params.append('deviceName', deviceName);
    
    const response = await fetch(`../wechatrp/getFaultDetails?${params.toString()}`, {
      method: 'GET',
      credentials: 'include'
    });
    
    const result = await response.json();
    
    if (result.status === 200 && result.data && Array.isArray(result.data)) {
      appState.faultDetails = result.data;
      // 渲染故障详情列表
      renderFaultDetailList();
    } else {
      alert(`获取故障详情失败：${result.msg || '未知错误'}`);
    }
  } catch (error) {
    console.error('加载故障详情失败:', error);
    alert('获取故障详情失败，请稍后重试');
  }
}

/**
 * 新增：渲染故障详情列表（关键修改：适配 faultDesc 字段 + 优化点击交互）
 */
function renderFaultDetailList() {
  DOM.faultDetailList.innerHTML = '';

  if (appState.faultDetails.length === 0) {
    const emptyItem = document.createElement('div');
    emptyItem.className = 'p-2 text-neutral text-center';
    emptyItem.textContent = '暂无故障详情数据';
    DOM.faultDetailList.appendChild(emptyItem);
  }

  // 1. 先渲染正常故障项
  appState.faultDetails.forEach((fault, index) => {
    const faultDesc = fault.faultDesc || '未命名故障';
    const item = document.createElement('div');
    item.className = 'p-2 border border-gray-200 rounded-lg cursor-pointer hover:border-primary hover:bg-primary/5 transition-all';
    item.innerHTML = `
      <input type="radio" name="faultDetail" id="fault_${index}" value="${faultDesc}" class="mr-2">
      <label for="fault_${index}">${faultDesc}</label>
    `;

    item.addEventListener('click', (e) => {
      e.stopPropagation();
      const radioInput = item.querySelector('input');
      radioInput.checked = true;
      appState.selectedFaultDetail = faultDesc;

      document.querySelectorAll('[name="faultDetail"]').forEach(input => {
        const parent = input.closest('div');
        parent.classList.remove('border-primary', 'bg-primary/5');
      });
      item.classList.add('border-primary', 'bg-primary/5');
    });

    if (index === 0) {
      const radioInput = item.querySelector('input');
      radioInput.checked = true;
      item.classList.add('border-primary', 'bg-primary/5');
      appState.selectedFaultDetail = faultDesc;
    }

    DOM.faultDetailList.appendChild(item);
  });

  // 2. 追加【其他（手动输入）】选项
  const otherItem = document.createElement('div');
  otherItem.className = 'p-2 border border-gray-200 rounded-lg cursor-pointer hover:border-primary hover:bg-primary/5 transition-all mt-2';
  otherItem.innerHTML = `
    <input type="radio" name="faultDetail" id="fault_other" value="__OTHER__" class="mr-2">
    <label for="fault_other">其他（手动输入）</label>
  `;

otherItem.addEventListener('click', (e) => {
  e.stopPropagation();
  const radioInput = otherItem.querySelector('input');
  radioInput.checked = true;
  appState.selectedFaultDetail = '__OTHER__'; // 标记为手动输入

  // 核心新增：选中“其他”时清空故障信息输入框
  DOM.faultInformation.value = '';
  // 同步更新字符计数（避免计数显示异常）
  DOM.faultInformation.dispatchEvent(new Event('input'));

  document.querySelectorAll('[name="faultDetail"]').forEach(input => {
    const parent = input.closest('div');
    parent.classList.remove('border-primary', 'bg-primary/5');
  });
  otherItem.classList.add('border-primary', 'bg-primary/5');
});

  DOM.faultDetailList.appendChild(otherItem);
}

/**
 * 新增：初始化故障详情模态框
 */
function initFaultDetailModal() {
  // 关闭模态框
  DOM.closeFaultModalBtn.addEventListener('click', hideFaultDetailModal);
  
  // 点击模态框外部关闭
  DOM.faultDetailModal.addEventListener('click', (e) => {
    if (e.target === DOM.faultDetailModal) {
      hideFaultDetailModal();
    }
  });
  
// 确认选择故障详情
DOM.confirmFaultDetailBtn.addEventListener('click', () => {
  if (appState.selectedFaultDetail && appState.selectedFaultDetail !== '__OTHER__') {
    // 只有不是【其他】时才回填
    DOM.faultInformation.value = appState.selectedFaultDetail;
    DOM.faultInformation.dispatchEvent(new Event('input'));
  }
  hideFaultDetailModal();
});
}

/**
 * 新增：显示故障详情模态框
 */
function showFaultDetailModal() {
  DOM.faultDetailModal.classList.remove('hidden');
  DOM.faultDetailModal.classList.add('flex');
  document.body.style.overflow = 'hidden';
}

/**
 * 新增：隐藏故障详情模态框
 */
function hideFaultDetailModal() {
  DOM.faultDetailModal.classList.remove('flex');
  DOM.faultDetailModal.classList.add('hidden');
  document.body.style.overflow = '';
}

/**
 * 设备检测（识别三星手机）
 */
function initDeviceDetection() {
  const ua = navigator.userAgent.toLowerCase();
  appState.isSamsungDevice = /samsung|sm-|galaxy/i.test(ua);
  console.log('检测到三星设备：', appState.isSamsungDevice);
}

/**
 * 解析百度地图逆地理编码结果，获取最详细的地址和位置名称
 */
function parseGeocodeResult(result) {
  let fullAddress = '';
  let locationName = '';

  // 优先提取POI名称
  if (result.surroundingPois && result.surroundingPois.length > 0) {
    locationName = result.surroundingPois[0].title;
  } else if (result.poiList && result.poiList.length > 0) {
    locationName = result.poiList[0].name;
  }

  // 处理原始地址，去重重复的省市
  const rawAddress = result.address || '';
  const dedupedRawAddress = rawAddress.replace(/([省市区县]+)\1+/g, '$1');

  // 拼接详细地址
  if (result.addressComponents) {
    const { province, city, district, street, streetNumber } = result.addressComponents;
    const addressParts = [];
    
    if (province && !dedupedRawAddress.includes(province)) addressParts.push(province);
    if (city && !dedupedRawAddress.includes(city) && !addressParts.includes(city)) addressParts.push(city);
    if (district && !dedupedRawAddress.includes(district) && !addressParts.includes(district)) addressParts.push(district);
    if (street) addressParts.push(street);
    if (streetNumber) addressParts.push(streetNumber);
    
    fullAddress = dedupedRawAddress || addressParts.filter(Boolean).join('');
    fullAddress = fullAddress || dedupedRawAddress;
  } else {
    fullAddress = dedupedRawAddress;
  }

  // 补充POI名称
  if (locationName && fullAddress && !fullAddress.includes(locationName)) {
    fullAddress = `${fullAddress}（${locationName}）`;
  } else if (locationName && !fullAddress) {
    fullAddress = locationName;
    locationName = '';
  }

  // 提取核心位置名称
  if (!locationName) {
    const bracketMatch = fullAddress.match(/\(([^)]+)\)/);
    if (bracketMatch) {
      locationName = bracketMatch[1];
      fullAddress = fullAddress.replace(/\([^)]+\)/, '').trim();
    } else {
      const addressMatch = fullAddress.match(/(区|街道|路|巷|大道|小区|大厦|学校)(.+)$/);
      locationName = addressMatch && addressMatch[2] ? addressMatch[2].trim() : fullAddress || '精准定位位置';
    }
  }

  // 最终兜底
  locationName = locationName.trim().replace(/([省市区县]+)\1+/g, '$1') || '精准定位位置';
  fullAddress = fullAddress.trim().replace(/([省市区县]+)\1+/g, '$1') || locationName;

  console.log('地址解析结果（去重后）：', { fullAddress, locationName });
  return { fullAddress, locationName };
}

/**
 * 定位按钮点击处理（优化12：增加加载状态防重复点击）
 */
async function handleLocateClick() {
  console.log('定位按钮被点击');
  
  // 防止重复点击
  if (appState.isMapLoading) return;
  
  if (appState.isiOSWechat && window.location.protocol !== 'https:') {
    showLocationStatus('iOS微信要求HTTPS环境才能定位，请切换到HTTPS访问', 'error');
    return;
  }

  DOM.locateBtn.disabled = true;
  DOM.locateBtn.innerHTML = '<i class="fa fa-spinner fa-spin"></i>';

  try {
    showLocationStatus('正在加载地图资源，请稍候...', 'info');
    DOM.bracketedAddressText.textContent = '地图加载中...';
    
    // 优化13：地图脚本只加载一次
    await loadMapScript();
    appState.mapLoaded = true;
    startLocation();
  } catch (error) {
    console.error('定位按钮点击异常:', error);
    showLocationStatus(`地图加载失败：${error.message}，请手动输入地址`, 'error');
    DOM.bracketedAddressText.textContent = '地图加载失败';
    resetLocateButton();
  }
}

/**
 * 基础表单初始化
 */
function initFormBasic() {
  // 设置默认日期
  const now = new Date();
  now.setDate(now.getDate() + 1);
  DOM.expectedVisitTimeInput.value = now.toISOString().slice(0, 10);

  // 设置设备年份范围
  const currentYear = new Date().getFullYear();
  DOM.equipmentYearInput.max = currentYear + 1;
  DOM.equipmentYearInput.placeholder = `请输入设备购买/出厂年份（1990-${currentYear + 1}）`;

  // 故障描述字符计数（优化14：防抖处理）
  const updateCharCount = debounce(() => {
    let length = DOM.faultInformation.value.length;
    if (length > 500) {
      DOM.faultInformation.value = DOM.faultInformation.value.substring(0, 500);
      length = 500;
    }
    DOM.charCount.textContent = `${length}/500`;
    DOM.charCount.classList.toggle('text-danger', length > 500);
  }, 100);
  
  DOM.faultInformation.addEventListener('input', updateCharCount);

  // 手机号验证（优化15：防抖处理）
  const validatePhone = debounce(function() {
    const value = this.value.trim();
    if (!value) {
      DOM.contactNumberError.classList.add('hidden');
      this.classList.remove('border-danger');
      this.classList.add('border-gray-300');
      return;
    }
    
    // 关键修改：优化正则表达式，精准匹配手机号和座机号
    // 规则：
    // 1. 手机号：11位，以13-19开头
    // 2. 座机号：8位纯数字 或 区号+8位数字（支持-分隔/空格分隔）
    const phoneRegex = /^(1[3-9]\d{9}|\d{8}|0\d{2,3}[- ]?\d{8})$/;
    
    if (phoneRegex.test(value)) {
      DOM.contactNumberError.classList.add('hidden');
      this.classList.remove('border-danger');
      this.classList.add('border-gray-300');
    } else {
      // 修改提示文本，明确告知支持的格式
      DOM.contactNumberError.textContent = '请输入有效的手机号（11位）或座机号（8位纯数字/区号+8位）';
      DOM.contactNumberError.classList.remove('hidden');
      this.classList.remove('border-gray-300');
      this.classList.add('border-danger');
    }
  }, 200);
  
  DOM.contactNumberInput.addEventListener('input', validatePhone);

  // 帮助弹窗
  DOM.helpBtn.addEventListener('click', () => {
    DOM.helpModal.classList.remove('hidden');
    DOM.helpModal.classList.add('flex');
    document.body.style.overflow = 'hidden';
  });
  
  DOM.closeHelpBtn.addEventListener('click', closeHelpModal);
  DOM.helpModal.addEventListener('click', (e) => {
    if (e.target === DOM.helpModal) closeHelpModal();
  });

  // 确认位置按钮
  DOM.confirmLocationBtn.addEventListener('click', () => {
    DOM.mapContainer.classList.add('hidden');
    const addressValue = DOM.schoolAddress.value.trim();
    showLocationStatus(addressValue ? `位置已确认: <strong>${addressValue}</strong>` : '位置已确认，但地址为空', 'success');
    resetLocateButton();
  });

  // 表单提交
  DOM.repairForm.addEventListener('submit', handleFormSubmit);
}

/**
 * 环境检测（iOS微信判断）
 */
function initEnvCheck() {
  const ua = navigator.userAgent.toLowerCase();
  appState.isiOSWechat = /iphone|ipad|ipod/.test(ua) && /micromessenger/.test(ua);
  console.log('检测到iOS微信环境:', appState.isiOSWechat);
  
  if (appState.isiOSWechat) {
    DOM.iosWechatTip.classList.remove('hidden');
    if (window.location.protocol !== 'https:') {
      showLocationStatus('iOS微信要求HTTPS环境才能定位，请切换到HTTPS访问', 'error');
    }
  }
}

/**
 * 定位权限检查
 */
function checkLocationPermission() {
  if (navigator.permissions && navigator.permissions.query) {
    navigator.permissions.query({ name: 'geolocation' }).then(result => {
      console.log('定位权限状态:', result.state);
      if (result.state === 'denied') {
        const tip = appState.isiOSWechat ?
          '定位权限已被拒绝，请在微信设置中开启（微信→我→设置→隐私→定位服务）' :
          '定位权限已被拒绝，请在浏览器设置中开启';
        showLocationStatus(tip, 'error');
      }
      
      result.addEventListener('change', () => {
        console.log('定位权限状态变更:', result.state);
        if (result.state === 'granted') {
          showLocationStatus('定位权限已开启，可点击定位按钮重试', 'success');
        }
      });
    }).catch(err => {
      console.error('检测定位权限失败:', err);
    });
  }
}

/**
 * 加载百度地图脚本（优化16：单例模式，只加载一次）
 */
function loadMapScript() {
  console.log('开始加载百度地图脚本');
  
  // 如果已加载，直接返回
  if (appState.mapScriptLoaded) {
    return Promise.resolve();
  }
  
  // 如果正在加载，等待加载完成
  if (appState.isMapLoading) {
    return new Promise((resolve, reject) => {
      const checkLoaded = setInterval(() => {
        if (appState.mapScriptLoaded) {
          clearInterval(checkLoaded);
          resolve();
        } else if (!appState.isMapLoading) {
          clearInterval(checkLoaded);
          reject(new Error('地图加载被取消'));
        }
      }, 100);
      
      setTimeout(() => {
        clearInterval(checkLoaded);
        reject(new Error('地图加载超时'));
      }, 10000);
    });
  }

  // 如果全局已存在BMap，标记为已加载
  if (window.BMap) {
    console.log('百度地图已加载，直接返回');
    appState.BMap = window.BMap;
    appState.geocoder = new appState.BMap.Geocoder();
    appState.mapScriptLoaded = true;
    return Promise.resolve();
  }

  appState.isMapLoading = true;
  
  return new Promise((resolve, reject) => {
    // 检查是否已有脚本标签
    const existingScript = document.querySelector('script[src*="api.map.baidu.com"]');
    if (existingScript) {
      const checkLoaded = setInterval(() => {
        if (window.BMap) {
          clearInterval(checkLoaded);
          appState.BMap = window.BMap;
          appState.geocoder = new appState.BMap.Geocoder();
          appState.mapScriptLoaded = true;
          appState.isMapLoading = false;
          resolve();
        }
      }, 100);
      
      setTimeout(() => {
        clearInterval(checkLoaded);
        appState.isMapLoading = false;
        reject(new Error('地图加载超时'));
      }, 10000);
      return;
    }

    // 创建新脚本标签
    const script = document.createElement('script');
    script.src = 'https://api.map.baidu.com/api?v=2.0&ak=O1etBmdvVAYUTTR3S3nZz2l0S4br8v4p&callback=initBaiduMap';
    script.async = true;
    script.defer = true;

    window.initBaiduMap = () => {
      console.log('百度地图脚本加载完成');
      appState.BMap = window.BMap;
      appState.geocoder = new appState.BMap.Geocoder();
      appState.mapScriptLoaded = true;
      appState.isMapLoading = false;
      resolve();
    };

    script.onerror = () => {
      console.error('百度地图脚本加载失败');
      appState.isMapLoading = false;
      reject(new Error('地图加载失败'));
    };

    setTimeout(() => {
      if (appState.isMapLoading) {
        console.error('百度地图脚本加载超时');
        appState.isMapLoading = false;
        reject(new Error('地图脚本加载超时'));
      }
    }, 10000);

    document.head.appendChild(script);
  });
}

/**
 * 高精度定位
 */
function startLocation() {
  console.log('开始高精度定位');
  
  if (!navigator.geolocation) {
    showLocationStatus('您的浏览器不支持地理定位，请手动输入地址', 'error');
    resetLocateButton();
    return;
  }

  showLocationStatus('正在高精度定位...请允许定位权限', 'info');
  DOM.bracketedAddressText.textContent = '高精度定位中...';

  const geoOptions = {
    enableHighAccuracy: true,
    timeout: appState.isiOSWechat ? 20000 : 15000,
    maximumAge: 0
  };

  let retryCount = 0;
  const maxRetry = 2;
  
  const getLocation = () => {
    navigator.geolocation.getCurrentPosition((position) => {
      console.log('定位成功，原始坐标（WGS84）：', position.coords.latitude, position.coords.longitude);
      const latitude = position.coords.latitude;
      const longitude = position.coords.longitude;
      fetchAddressFromBackend(latitude, longitude);
    }, (error) => {
      console.error('定位失败:', error);
      retryCount++;
      
      if (retryCount <= maxRetry) {
        showLocationStatus(`定位失败，正在重试（${retryCount}/${maxRetry}）...`, 'info');
        setTimeout(getLocation, 1000);
        return;
      }
      
      let message = '';
      switch (error.code) {
        case error.PERMISSION_DENIED:
          message = appState.isiOSWechat ?
            '您拒绝了定位权限，请在微信设置中开启（微信→我→设置→隐私→定位服务）' :
            '您拒绝了定位权限，请在浏览器设置中开启';
          break;
        case error.POSITION_UNAVAILABLE:
          message = '位置信息不可用，请手动输入地址或稍后重试';
          break;
        case error.TIMEOUT:
          message = '定位超时，请手动输入地址或稍后重试';
          break;
        case error.UNKNOWN_ERROR:
          message = '定位失败，请手动输入地址或稍后重试';
          break;
      }
      
      showLocationStatus(message, 'error');
      DOM.bracketedAddressText.textContent = '定位失败';
      resetLocateButton();
      initMiniMap(39.9042, 116.4074);
    }, geoOptions);
  };

  getLocation();
}

/**
 * 从后端获取高精度地址
 */
function fetchAddressFromBackend(latitude, longitude) {
  console.log('请求后端解析地址，原始坐标（WGS84）：', latitude, longitude);
  showLocationStatus('正在解析高精度地址...', 'info');
  DOM.bracketedAddressText.textContent = '地址解析中...';
  
  const params = new URLSearchParams();
  params.append('latitude', latitude);
  params.append('longitude', longitude);
  
  fetch(`../wechatrp/geocode?${params.toString()}`, {
    method: 'GET',
    mode: 'cors',
    credentials: 'include'
  })
  .then(response => {
    console.log('后端接口响应状态:', response.status);
    if (!response.ok) throw new Error(`接口请求失败，状态码：${response.status}`);
    return response.json();
  })
  .then(data => {
    console.log('后端返回地址数据:', JSON.stringify(data));
    
    if (data && data.status === 200 && data.address && data.bdLat && data.bdLng) {
      const mockResult = {
        address: data.address,
        addressComponents: data.addressComponents || {},
        surroundingPois: data.surroundingPois || [],
        poiList: data.poiList || []
      };
      
      const { fullAddress, locationName } = parseGeocodeResult(mockResult);
      DOM.schoolAddress.value = fullAddress;
      DOM.bracketedAddressText.textContent = locationName;
      showLocationStatus(`高精度地址获取成功：${locationName}`, 'info');
      
      initMiniMap(data.bdLat, data.bdLng, fullAddress);
    } else {
      console.warn('后端返回数据不完整，降级到前端解析：', data);
      fallbackToFrontendGeocode(latitude, longitude);
    }
  })
  .catch(error => {
    console.error('后端地址解析失败，降级到前端解析：', error);
    showLocationStatus('后端地址解析失败，尝试前端解析...', 'info');
    fallbackToFrontendGeocode(latitude, longitude);
  })
  .finally(() => {
    resetLocateButton();
  });
}

/**
 * 后端解析失败时，使用百度地图前端逆地理编码兜底
 */
function fallbackToFrontendGeocode(lat, lng) {
  if (!appState.BMap || !appState.geocoder) {
    console.error('百度地图未加载，无法前端解析');
    showLocationStatus('地址解析失败，请手动输入地址', 'error');
    DOM.bracketedAddressText.textContent = "地址解析失败";
    initMiniMap(lat, lng);
    return;
  }

  // 坐标转换
  const convertor = new appState.BMap.Convertor();
  const pointArr = [new appState.BMap.Point(lng, lat)];
  
  convertor.translate(pointArr, 1, 5, (res) => {
    let bdPoint;
    if (res.status === 0) {
      bdPoint = res.points[0];
      console.log('坐标转换成功（WGS84→BD09）：', bdPoint.lng, bdPoint.lat);
    } else {
      bdPoint = new appState.BMap.Point(lng, lat);
      console.warn('坐标转换失败，使用原始坐标');
    }

    // 逆地理编码
    appState.geocoder.getLocation(bdPoint, (result) => {
      if (result && (result.address || result.addressComponents)) {
        const { fullAddress, locationName } = parseGeocodeResult(result);
        DOM.schoolAddress.value = fullAddress;
        DOM.bracketedAddressText.textContent = locationName;
        showLocationStatus(`地址解析成功：${locationName}`, 'info');
        initMiniMap(bdPoint.lat, bdPoint.lng, fullAddress);
      } else {
        const coordText = `${bdPoint.lat.toFixed(6)}, ${bdPoint.lng.toFixed(6)}`;
        DOM.schoolAddress.value = coordText;
        DOM.bracketedAddressText.textContent = "经纬度定位";
        showLocationStatus('仅获取到经纬度，请手动补充详细地址', 'warning');
        initMiniMap(bdPoint.lat, bdPoint.lng, coordText);
      }
    });
  });
}

/**
 * 初始化迷你地图（优化17：减少重复DOM操作）
 */
function initMiniMap(lat, lng, address) {
  console.log('初始化迷你地图（BD09坐标）：', lat, lng, '三星设备：', appState.isSamsungDevice);
  
  if (!appState.BMap || !appState.geocoder) {
    showLocationStatus('地图加载失败，无法显示', 'error');
    return;
  }
  
  DOM.mapContainer.classList.remove('hidden');
  
  // 地图配置
  const mapOptions = {
    enableMapClick: true,
    enableDragging: true,
    enableScrollWheelZoom: true,
    enableContinuousZoom: true,
    enablePinchToZoom: true,
    disableDoubleClickZoom: false
  };
  
  // 清理旧地图
  if (appState.map) {
    appState.map.clearOverlays();
    appState.map = null;
  }
  
  // 创建新地图
  appState.map = new appState.BMap.Map("miniMap", mapOptions);
  const bdPoint = new appState.BMap.Point(lng, lat);
  const zoomLevel = appState.isSamsungDevice ? 17 : 18;
  
  appState.map.centerAndZoom(bdPoint, zoomLevel);
  appState.map.enableScrollWheelZoom(true);
  appState.map.enableDragging(true);
  appState.map.enableContinuousZoom(true);
  appState.map.enablePinchToZoom(true);
  appState.map.disableKeyboard();

  // 延迟初始化标记
  setTimeout(() => {
    if (appState.marker) appState.map.removeOverlay(appState.marker);
    
    // 创建标记
    appState.marker = new appState.BMap.Marker(bdPoint, {
      draggable: true,
      raiseOnDrag: true,
      enableMassClear: false
    });
    
    appState.map.addOverlay(appState.marker);
    appState.marker.setAnimation(appState.BMap_ANIMATION_DROP || 1);

    // 更新地址
    if (address) {
      const { locationName } = parseGeocodeResult({ address });
      DOM.schoolAddress.value = address;
      DOM.bracketedAddressText.textContent = locationName;
    }

    // 三星设备特殊处理
    if (appState.isSamsungDevice) {
      appState.marker.addEventListener('touchstart', function(e) {
        e.preventDefault();
        this.startDrag();
      });
      
      appState.map.getContainer().addEventListener('touchmove', function(e) {
        if (!e._isProcessed) {
          e._isProcessed = true;
          e.stopPropagation();
        }
      }, { passive: true });
    }

    // 标记拖动事件
    appState.marker.addEventListener('dragend', function(e) {
      const newPoint = e.point;
      appState.geocoder.getLocation(newPoint, updateAddressByPoint);
    });

    // 地图点击事件
    appState.map.addEventListener('click', function(e) {
      const clickPoint = e.point;
      appState.marker.setPosition(clickPoint);
      appState.map.panTo(clickPoint);
      appState.geocoder.getLocation(clickPoint, updateAddressByPoint);
    });
    
    // 三星设备地图拖动事件
    if (appState.isSamsungDevice) {
      appState.map.addEventListener('dragend', function() {
        const centerPoint = appState.map.getCenter();
        appState.marker.setPosition(centerPoint);
        appState.geocoder.getLocation(centerPoint, updateAddressByPoint);
      });
    }
  }, appState.isSamsungDevice ? 500 : 300);

  // iOS微信提示
  if (appState.isiOSWechat && !DOM.mapContainer.querySelector('.ios-wechat-tip')) {
    const iosTip = document.createElement('div');
    iosTip.className = 'ios-wechat-tip text-xs text-danger my-2 px-2 py-1 bg-danger/10 rounded';
    iosTip.innerHTML = '<i class="fa fa-info-circle mr-1"></i> iOS微信用户：若地图操作不流畅，请尝试缩小地图后再操作，或直接手动输入地址';
    DOM.mapContainer.querySelector('.bg-gray-50').prepend(iosTip);
  }
  
  // 三星设备提示
  if (appState.isSamsungDevice && !DOM.mapContainer.querySelector('.samsung-tip')) {
    const samsungTip = document.createElement('div');
    samsungTip.className = 'samsung-tip text-xs text-accent my-2 px-2 py-1 bg-accent/10 rounded';
    samsungTip.innerHTML = '<i class="fa fa-info-circle mr-1"></i> 三星用户：长按标记可拖动，点击地图快速定位';
    DOM.mapContainer.querySelector('.bg-gray-50').prepend(samsungTip);
  }
}

/**
 * 更新地址
 */
function updateAddressByPoint(result) {
  if (result && (result.address || result.addressComponents)) {
    const { fullAddress, locationName } = parseGeocodeResult(result);
    DOM.schoolAddress.value = fullAddress;
    DOM.bracketedAddressText.textContent = locationName;
    showLocationStatus(`地址已更新：${locationName}`, 'info');
    
    if (appState.map && appState.marker) {
      appState.map.panTo(appState.marker.getPosition());
    }
  } else {
    showLocationStatus('地址更新失败，请手动补充', 'error');
    DOM.bracketedAddressText.textContent = "地址更新失败";
  }
}

/**
 * 重置定位按钮
 */
function resetLocateButton() {
  try {
    console.log('重置定位按钮');
    DOM.locateBtn.disabled = false;
    DOM.locateBtn.innerHTML = '<i class="fa fa-map-marker"></i>';
    appState.isMapLoading = false;
  } catch (error) {
    console.error('重置定位按钮失败:', error);
  }
}

/**
 * 显示定位状态提示
 */
function showLocationStatus(message, type) {
  DOM.locationStatus.classList.remove('hidden', 'text-success', 'text-danger', 'text-primary', 'text-accent');
  
  switch (type) {
    case 'success':
      DOM.locationStatus.classList.add('text-success');
      DOM.locationStatus.innerHTML = `<i class="fa fa-check-circle mr-1"></i> ${message}`;
      break;
    case 'error':
      DOM.locationStatus.classList.add('text-danger');
      DOM.locationStatus.innerHTML = `<i class="fa fa-exclamation-circle mr-1"></i> ${message}`;
      DOM.locationStatus.style.whiteSpace = 'pre-line';
      break;
    case 'warning':
      DOM.locationStatus.classList.add('text-accent');
      DOM.locationStatus.innerHTML = `<i class="fa fa-exclamation-triangle mr-1"></i> ${message}`;
      break;
    default:
      DOM.locationStatus.classList.add('text-primary');
      DOM.locationStatus.innerHTML = `<i class="fa fa-info-circle mr-1"></i> ${message}`;
  }
  
  DOM.locationStatus.classList.remove('hidden');
}

// ==============================================
// 新增：图片压缩函数（自动压缩到 1MB 以内，质量 0.7）
// ==============================================
function compressImage(file) {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = (e) => {
      const img = new Image();
      img.src = e.target.result;
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        // 最大宽度 1920，等比缩放
        const maxSize = 1920;
        if (width > height && width > maxSize) {
          height = (height * maxSize) / width;
          width = maxSize;
        } else if (height > maxSize) {
          width = (width * maxSize) / height;
          height = maxSize;
        }

        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);

        // 转 blob，质量 0.7
        canvas.toBlob(
          (blob) => {
            resolve(blob);
          },
          'image/jpeg',
          0.7
        );
      };
    };
  });
}

// ==============================================
// 重写：图片选择 → 自动压缩 → 再预览
// ==============================================
async function handleFileSelect(event) {
  const files = event.target.files;
  if (!files || files.length === 0) return;

  const totalCount = appState.uploadedFiles.length + files.length;
  if (totalCount > 5) {
    alert(`最多只能上传5张图片，当前已选择${appState.uploadedFiles.length}张，还可上传${5 - appState.uploadedFiles.length}张`);
    event.target.value = '';
    return;
  }

  DOM.previewContainer.classList.remove('hidden');
  DOM.photoCount.classList.remove('hidden');
  const fragment = document.createDocumentFragment();

  for (const file of Array.from(files)) {
    if (!file.type.startsWith('image/')) {
      alert('请上传图片文件（JPG/PNG格式）');
      continue;
    }

    // ======================
    // 关键：自动压缩图片
    // ======================
    const compressedBlob = await compressImage(file);
    const compressedFile = new File([compressedBlob], `compressed_${file.name}`, {
      type: 'image/jpeg',
    });

    // 把压缩后的图片加入列表
    appState.uploadedFiles.push(compressedFile);

    // 预览图（不变）
    const previewItem = document.createElement('div');
    previewItem.className = 'relative group bg-gray-100 rounded-lg overflow-hidden aspect-square';
    previewItem.dataset.index = appState.uploadedFiles.length - 1;

    const reader = new FileReader();
    reader.onload = (e) => {
      const img = document.createElement('img');
      img.src = e.target.result;
      img.className = 'w-full h-full object-cover';

      const deleteBtn = document.createElement('button');
      deleteBtn.type = 'button';
      deleteBtn.className = 'absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white';
      deleteBtn.innerHTML = '<i class="fa fa-trash text-xl"></i>';
      deleteBtn.onclick = () => {
        const idx = parseInt(previewItem.dataset.index);
        appState.uploadedFiles.splice(idx, 1);
        previewItem.remove();
        updatePreviewIndexes();
        updatePhotoCount();
        if (appState.uploadedFiles.length === 0) {
          DOM.previewContainer.classList.add('hidden');
          DOM.photoCount.classList.add('hidden');
        }
      };

      const size = (compressedFile.size / 1024).toFixed(1);
      const name = compressedFile.name.length > 8 ? `${compressedFile.name.substring(0, 8)}...` : compressedFile.name;
      const info = document.createElement('div');
      info.className = 'absolute bottom-0 left-0 right-0 bg-black/60 text-white text-xs p-1 truncate';
      info.textContent = `${name} (${size}KB)`;

      previewItem.appendChild(img);
      previewItem.appendChild(deleteBtn);
      previewItem.appendChild(info);
      fragment.appendChild(previewItem);
      DOM.previewContainer.appendChild(fragment);
    };

    reader.readAsDataURL(compressedBlob);
  }

  updatePhotoCount();
  event.target.value = '';
}

/**
 * 更新预览图索引
 */
function updatePreviewIndexes() {
  const previewItems = DOM.previewContainer.querySelectorAll('.aspect-square');
  previewItems.forEach((item, index) => {
    item.dataset.index = index;
  });
}

/**
 * 更新图片计数
 */
function updatePhotoCount() {
  DOM.photoCount.textContent = `已选择 ${appState.uploadedFiles.length}/5 张图片`;
  DOM.photoCount.classList.toggle('text-accent', appState.uploadedFiles.length >= 5);
}

/**
 * 表单提交处理（优化20：减少重复DOM查询）
 */
function handleFormSubmit(e) {
  e.preventDefault();
  
  // 验证手机号（增强：主动触发验证）
  DOM.contactNumberInput.dispatchEvent(new Event('input'));
  if (!DOM.contactNumberError.classList.contains('hidden')) {
    DOM.contactNumberInput.focus();
    alert('请检查您输入的联系电话格式是否正确。支持：11位手机号、8位座机号、区号+8位座机号');
    return;
  }

  // 格式化手机号
  const contactValue = DOM.contactNumberInput.value.trim();
  const formattedContact = contactValue.replace(/\D/g, '');
  DOM.contactNumberInput.value = formattedContact;

  DOM.submitBtn.disabled = true;
  DOM.submitBtn.innerHTML = '<i class="fa fa-spinner fa-spin mr-2"></i> 提交中...';

  // 构建FormData
  const formData = new FormData(DOM.repairForm);
  formData.delete('photosInput');
  
  appState.uploadedFiles.forEach((file, index) => {
    formData.append('photos', file);
  });

  // 提交表单
  fetch('../wechatrp/wxRepair', {
    method: 'POST',
    body: formData,
    credentials: 'include'
  })
  .then(response => {
    if (!response.ok) throw new Error(`网络错误：状态码${response.status}`);
    return response.json();
  })
  .then(data => {
    if (data.status === 200) {
      showSuccessToast();
      setTimeout(() => {
        window.location.href = '../wechatrp/repairlist';
      }, 1500);
    } else {
      alert(`提交失败：${data.msg || '未知错误'}`);
    }
  })
  .catch(error => {
    console.error('提交失败详情:', error);
    alert(`提交失败：${error.message || '网络异常，请稍后重试'}`);
  })
  .finally(() => {
    DOM.submitBtn.disabled = false;
    DOM.submitBtn.innerHTML = '<i class="fa fa-paper-plane mr-2"></i> 提交报修';
  });
}

/**
 * 显示提交成功提示
 */
function showSuccessToast() {
  DOM.successToast.classList.remove('hidden', 'opacity-0');
  DOM.successToast.classList.add('opacity-100');
  
  setTimeout(() => {
    DOM.successToast.classList.remove('opacity-100');
    DOM.successToast.classList.add('opacity-0');
    
    setTimeout(() => {
      DOM.successToast.classList.add('hidden');
    }, 300);
  }, 3000);
}

/**
 * 关闭帮助弹窗
 */
function closeHelpModal() {
  DOM.helpModal.classList.remove('flex');
  DOM.helpModal.classList.add('hidden');
  document.body.style.overflow = '';
}

// 禁止页面缩放
document.addEventListener('keydown', function(e) {
  if ((e.ctrlKey || e.metaKey) && (e.key === '+' || e.key === '-')) {
    e.preventDefault();
  }
  
  if ((e.ctrlKey || e.metaKey) && e.key === '0') {
    e.preventDefault();
  }
});

// 全局触摸事件兼容
document.addEventListener('touchmove', function(e) {
  if (e.target.closest('#miniMap')) {
    e.stopPropagation();
  }
}, { passive: true });