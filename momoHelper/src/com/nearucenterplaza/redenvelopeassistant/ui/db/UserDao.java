package com.nearucenterplaza.redenvelopeassistant.ui.db;

import java.sql.SQLException;
import java.util.List;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.nearucenterplaza.redenvelopeassistant.ui.bean.User;

public class UserDao
{
	private Context context;
	private Dao<User, Integer> userDaoOpe;
	private DatabaseHelper helper;

	public UserDao(Context context)
	{
		this.context = context;
		try
		{
			helper = DatabaseHelper.getHelper(context);
			userDaoOpe = helper.getDao(User.class);
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 增加一个用户
	 * 
	 * @param user
	 * @throws SQLException
	 */
	public void add(User user) 
	{
		/*//事务操作
		TransactionManager.callInTransaction(helper.getConnectionSource(),
				new Callable<Void>()
				{

					@Override
					public Void call() throws Exception
					{
						return null;
					}
				});*/
		try
		{
			userDaoOpe.create(user);
		} catch (SQLException e)
		{
			e.printStackTrace();
		}

	}

	public User get(int id)
	{
		try
		{
			return userDaoOpe.queryForId(id);
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean getByName(String username)
	{
	    try {
            List<User> accountList =
                    userDaoOpe.query(
                            userDaoOpe.queryBuilder().where()
                    .eq("name", username)
                    .prepare());
            if(accountList != null && accountList.size()>0)
            {
                return true;
            }
        } catch (SQLException e) {
//            e.printStackTrace();
            return false;
        }
	    return false;
	}

}
