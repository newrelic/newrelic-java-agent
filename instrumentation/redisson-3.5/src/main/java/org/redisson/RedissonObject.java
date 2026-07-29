package org.redisson;

import org.redisson.api.RFuture;
import org.redisson.client.codec.Codec;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandAsyncService;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.Utils;

@Weave(type=MatchType.BaseClass)
public abstract class RedissonObject  {
	
	@NewField
	public String objectName = null;
	
	public RedissonObject(Codec codec, CommandAsyncExecutor commandExecutor, String name) {
		if(commandExecutor instanceof CommandAsyncService) {
			CommandAsyncService cmdAsyncService = (CommandAsyncService)commandExecutor;
			if(cmdAsyncService.objectType == null) {
				
				String clazzname = objectName != null ? objectName : getClass().getSimpleName().replace("Redisson", "");
				cmdAsyncService.objectType = clazzname;
			}
		}
	}
	public abstract String getName();

    public RFuture<Void> renameAsync(String newName) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RENAME");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Void> migrateAsync(String host, int port, int database) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("MIGRATE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> moveAsync(int database) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("MOVE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> renamenxAsync(String newName) {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("RENAMENX");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> deleteAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("DELETE");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Boolean> unlinkAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("UNLINK");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

    public RFuture<Boolean> touchAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("TOUCH");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }
    
    public RFuture<Boolean> isExistsAsync() {
		if(!Utils.operationIsSet()) {
			Utils.setOperation("ISEXISTS");
		}
		if(!Utils.typeSet()) {
			Utils.setType(this);
		}
		if(!Utils.objectNameSet()) {
			Utils.setObjectName(getName());
		}
		return Weaver.callOriginal();
    }

}
