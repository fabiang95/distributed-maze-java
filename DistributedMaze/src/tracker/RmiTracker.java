package tracker;

import message.PlayerBasicInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface RmiTracker extends Remote {
    int getN() throws RemoteException;
    int getK() throws RemoteException;
    ArrayList<PlayerBasicInfo> registerPlayer(PlayerBasicInfo p) throws RemoteException;
    ArrayList<PlayerBasicInfo> deRegisterPlayer(PlayerBasicInfo p) throws RemoteException;

}


