package com.jd.jdt.pe.pvm.ism;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/***
 * @param <T> T表示负载的类型
 */
public class State<T> {

    public Long id;

    //some infos contained in State,always mean some data related to Business
    public T payload;

    //the State created by some FuncDescriptor, means the FuncDescriptor that process the previous State and
    //create this new State prototyped with that old one
    public FuncDescriptor createdBy = FuncDescriptor.SYSTEM_FUNCDESCRIPTOR;

    public Long createdAt = System.currentTimeMillis();

    //All FuncDescriptor to be started one after another by Case's main loop
    public Set<FuncDescriptor> tobeStarted = new LinkedHashSet<>();

    //All FuncDescriptor waiting for msg to be appear
    public Set<FuncDescriptor> ing = new HashSet<>();

    //msg to be sent to ing set
    public Msg<?> toIngMsg = null;

    //All FuncDescriptor in ing that have received toIngMsg
    public Set<FuncDescriptor> alreadySend = new HashSet<>();


    //validate FuncDescriptor : in tobeStarted , or in ing , or SYSTEM_FUNCDESCRIPTOR
    public boolean IllegalFuncDescriptor(FuncDescriptor funcDescriptor) {
        return !(tobeStarted.contains(funcDescriptor) || ing.contains(funcDescriptor) || funcDescriptor.funcID.equals("SYSTEM"));
    }


    //noneToDo : tobeStarted is empty and ing is empty
    //always means finished
    //noneToDo means noneToSchedule
    public boolean noneToDo() {
        if (!tobeStarted.isEmpty()) return false;
        return ing.isEmpty();
    }

    //noneToSchedule : tobeStarted is empty and toIngMsg is null
    //always means the main loop is paused
    //noneToSchedule doesn't mean noneToDo,as ing may not be empty
    public boolean noneToSchedule() {
        if (!tobeStarted.isEmpty()) return false;
        return toIngMsg == null;
    }

    //always mean canceled or force to be finish
    public void setToNoneToDo() {
        //todo : to add debug log
        tobeStarted.clear();
        ing.clear();
    }

    //always mean canceled or force to be finish
    public void setToNoneToDo(FuncDescriptor except) {
        tobeStarted.removeIf(funcDescriptor -> !funcDescriptor.equals(except));
        ing.removeIf(funcDescriptor -> !funcDescriptor.equals(except));
    }

    //protoType one,means created a copy of this State
    public State<T> protoType() {
        State<T> state = new State<>();
        initState(state);
        return state;
    }

    //state with the id + 1
    public void initState(State<T> state){
        state.id = this.id + 1L;
        state.payload = this.payload;

        state.tobeStarted.addAll(this.tobeStarted);
        state.ing.addAll(this.ing);

        state.toIngMsg = this.toIngMsg;
        state.alreadySend.addAll(this.alreadySend);
    }

    @Override
    public String toString() {
        return "State{" +
                "id=" + id +
                ", other=" + payload +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", tobeStarted=" + tobeStarted +
                ", ing=" + ing +
                ", toIngMsg=" + toIngMsg +
                ", alreadySend=" + alreadySend +
                '}';
    }


}
