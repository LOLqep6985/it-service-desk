import fs from 'node:fs';

const dir = 'D:/DeepSeek_data/gradaute_space/it-service-desk/src/main/resources/static';

function patch(file, replacements) {
  const path = `${dir}/${file}`;
  let content = fs.readFileSync(path, 'utf8');
  let allOk = true;
  for (const [oldStr, newStr] of replacements) {
    if (!content.includes(oldStr)) {
      console.log(`MISS ${file}: ${oldStr.slice(0, 60)}`);
      allOk = false;
    } else {
      content = content.split(oldStr).join(newStr);
    }
  }
  if (allOk) {
    fs.writeFileSync(path, content, 'utf8');
    console.log(`OK ${file}`);
  }
}

const statusMapLine = `  const WORK_ORDER_STATUS = { PENDING:'待派单', ASSIGNED:'已派单', IN_PROGRESS:'处理中', COMPLETED:'已完成' };
  const CAMPUS_LABEL = { LAOYUAN:'老院', XIYUAN:'西院', DONGYUAN:'东院' };`;

const paramsLine = '  const params = new URLSearchParams(location.search);';

patch('today.html', [
  [paramsLine, statusMapLine + '\n' + paramsLine],
  ["      const key = wo.campus ? wo.campus.label : '其他';", "      const key = CAMPUS_LABEL[wo.campus] || '其他';"],
  ["          + '<div class=\"meta\">' + wo.departmentName + ' · ' + (wo.status ? wo.status.label : '') + '</div>'",
   "          + '<div class=\"meta\">' + wo.departmentName + ' · ' + (WORK_ORDER_STATUS[wo.status] || '') + '</div>'"],
]);

patch('detail.html', [
  [paramsLine, statusMapLine + '\n' + paramsLine],
  ["      ['状态', '<span class=\"badge\">' + esc(wo.status.label) + '</span>'],",
   "      ['状态', '<span class=\"badge\">' + esc(WORK_ORDER_STATUS[wo.status] || wo.status) + '</span>'],"],
  ["      ['院区', esc(wo.campus.label)],",
   "      ['院区', esc(CAMPUS_LABEL[wo.campus] || wo.campus)],"],
  ["    if (wo.status.label === '已完成') {",
   "    if (WORK_ORDER_STATUS[wo.status] === '已完成') {"],
]);

patch('create.html', [
  [paramsLine, '  const CAMPUS_LABEL = { LAOYUAN:\'老院\', XIYUAN:\'西院\', DONGYUAN:\'东院\' };\n' + paramsLine],
  ["        opt.textContent = d.name + '（' + d.campus.label + '）';",
   "        opt.textContent = d.name + '（' + (CAMPUS_LABEL[d.campus] || d.campus) + '）';"],
]);

console.log('done');