package zhongchiedu.inventory.service.Impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import zhongchiedu.common.utils.BasicDataResult;
import zhongchiedu.common.utils.Common;
import zhongchiedu.framework.service.GeneralServiceImpl;
import zhongchiedu.inventory.pojo.Column;
import zhongchiedu.inventory.service.ColumnService;
import zhongchiedu.log.annotation.SystemServiceLog;

@Service
@Slf4j
public class ColumnServiceImpl extends GeneralServiceImpl<Column> implements ColumnService {
	@Override
	@SystemServiceLog(description="根据标识获取所有列")
	public List<String> findColumns(String name,String userId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("userId").is(userId));
		Column column = this.findOneByQuery(query, Column.class);
		if(Common.isNotEmpty(column)){
			return column.getColumns();
		}else{
			List<String> list = new ArrayList();
			list.add("name");
			return list;
		}
		
		
	}

	@Override
	@SystemServiceLog(description="编辑列")
	public BasicDataResult editColumns(String name,String showcolumn,boolean flag,String userId) {
		Query query = new Query();
		query.addCriteria(Criteria.where("name").is(name));
		query.addCriteria(Criteria.where("userId").is(userId));
		Column column = this.findOneByQuery(query, Column.class);
		if(Common.isEmpty(column)){
			//新建一个
			column = new Column();
			List<String> list = new ArrayList<>();
			list.add(showcolumn);
			column.setName(name);
			column.setColumns(list);
			column.setUserId(userId);
			this.insert(column);
			return  BasicDataResult.build(200, "添加成功", showcolumn);
		}else{
			//更新
			List<String> list = column.getColumns();
			if(list.contains(showcolumn)){
				if(!flag){
					list.remove(showcolumn);
				}
			}else{
				list.add(showcolumn);
			}
			column.setColumns(list);
			column.setUserId(userId);
			this.save(column);
			return BasicDataResult.build(200, "修改成功", showcolumn);
		}
		

		
	}

	

}
