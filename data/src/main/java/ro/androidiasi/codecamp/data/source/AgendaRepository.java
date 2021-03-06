package ro.androidiasi.codecamp.data.source;

import android.util.Log;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;

import java.util.ArrayList;
import java.util.List;

import ro.androidiasi.codecamp.data.model.DataCodecamper;
import ro.androidiasi.codecamp.data.model.DataRoom;
import ro.androidiasi.codecamp.data.model.DataSession;
import ro.androidiasi.codecamp.data.model.DataSponsor;
import ro.androidiasi.codecamp.data.model.DataTimeFrame;
import ro.androidiasi.codecamp.data.source.local.AgendaLocalSnappyDataSource;
import ro.androidiasi.codecamp.data.source.local.exception.DataNotFoundException;
import ro.androidiasi.codecamp.data.source.remote.ConnectAPIRemoteDataSource;
import ro.androidiasi.codecamp.data.source.remote.FileRemoteDataSource;

/**
 * Created by andrei on 06/04/16.
 */
@EBean(scope = EBean.Scope.Singleton)
public class AgendaRepository implements IAgendaDataSource<Long> {

    private static final String TAG = "AgendaRepository";
    private DataConference mDataConference;
    @Bean AgendaLocalSnappyDataSource mLocalSnappyDataSource;
    @Bean FileRemoteDataSource mFileRemoteDataSource;
    @Bean ConnectAPIRemoteDataSource mWebViewRemoteDataSource;

    private List<DataRoom> mMemCacheDataRooms;
    private List<DataTimeFrame> mMemCacheTimeFrame;
    private List<DataCodecamper> mMemCacheDataCodecampers;
    private List<DataSession> mMemCacheDataSession;
    private List<DataSponsor> mMemCacheDataSponsors;
    private List<DataConference> mMemCacheDataConferences;

    @AfterInject public void afterMembersInject() {
    }

    public void ensureConferenceLoaded() {
        if (mDataConference == null) {
            this.getConferencesListSync(new ILoadCallback<List<DataConference>>() {
                @Override public void onSuccess(List<DataConference> pObject) {
                    setLatestConference(pObject);
                }

                @Override public void onFailure(Exception pException) {
                    // NO-OP?
                }
            });
            while (mDataConference == null){
                // Until I figure out why Background "serial" does not work, this will have to do.
            }
        }
    }

    @Background(serial = "serial")
    @Override
    public void getRoomsList(boolean pForced, final ILoadCallback<List<DataRoom>> pLoadCallback) {
        if (pForced) {
            this.invalidateDataRoomsList();
        }
        this.getRoomsFromRemote(pLoadCallback);
    }

    @Background(serial = "serial")
    @Override
    public void getSessionsList(boolean pForced, final ILoadCallback<List<DataSession>> pLoadCallback) {
        if (pForced) {
            this.invalidateDataSessions();
            this.getSessionsFromRemote(pLoadCallback);
        } else {
            this.getSessionsList(pLoadCallback);
        }
    }


    @Background(serial = "serial")
    @Override
    public void getFavoriteSessionsList(boolean pForced, final ILoadCallback<List<DataSession>> pLoadCallback) {
        this.getFavoriteSessionsList(pLoadCallback);
    }

    @Background(serial = "serial")
    @Override
    public void getTimeFramesList(boolean pForced, final ILoadCallback<List<DataTimeFrame>> pLoadCallback) {
        if (pForced) {
            this.inavlidateTimeFrameList();
        }
        this.getTimeFramesFromRemote(pLoadCallback);
    }

    @Background(serial = "serial")
    @Override
    public void getCodecampersList(boolean pForced, final ILoadCallback<List<DataCodecamper>> pLoadCallback) {
        if (pForced) {
            this.invalidateCodecampersList();
        }
        this.getCodecampersFromRemote(pLoadCallback);
    }

    @Background(serial = "serial")
    @Override
    public void getSponsorsList(boolean pForced, ILoadCallback<List<DataSponsor>> pLoadCallback) {
        if (pForced) {
            this.invalidateDataSponsors();
            this.getSponsorsFromRemote(pLoadCallback);
        } else {
            this.getSponsorsList(pLoadCallback);
        }
    }

    @Background(serial = "serial")
    @Override
    public void getConferencesList(boolean pForced, ILoadCallback<List<DataConference>> pLoadCallback) {
        if (pForced) {
            this.invalidateDataConferences();
            this.getConferencesFromRemote(pLoadCallback);
        } else {
            this.getConferencesList(pLoadCallback);
        }
    }

    @Background(serial = "serial")
    @Override public void getRoomsList(final ILoadCallback<List<DataRoom>> pLoadCallback) {
        ensureConferenceLoaded();
        if (mMemCacheDataRooms != null) {
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataRooms);
            return;
        }
        this.mLocalSnappyDataSource.getRoomsList(new ILoadCallback<List<DataRoom>>() {
            @Override public void onSuccess(List<DataRoom> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataRooms = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {

                getRoomsFromRemote(new ILoadCallback<List<DataRoom>>() {
                    @Override public void onSuccess(List<DataRoom> pObject) {
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                    }

                    @Override public void onFailure(Exception pException) {
                        mFileRemoteDataSource.getRoomsList(new ILoadCallback<List<DataRoom>>() {
                            @Override public void onSuccess(List<DataRoom> pObject) {
                                mMemCacheDataRooms = pObject;
                                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                            }

                            @Override public void onFailure(Exception pException) {
                                // Could not find the data AYNWHERE
                                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
                            }
                        });
                    }
                });
            }
        });
    }

    @Background(serial = "serial")
    @Override public void getSessionsList(final ILoadCallback<List<DataSession>> pLoadCallback) {
        ensureConferenceLoaded();
        if (mMemCacheDataSession != null) {
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataSession);
            return;
        }
        this.mLocalSnappyDataSource.getSessionsList(new ILoadCallback<List<DataSession>>() {
            @Override public void onSuccess(List<DataSession> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSession = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                getSessionsFromRemote(new ILoadCallback<List<DataSession>>() {
                    @Override public void onSuccess(List<DataSession> pObject) {
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                    }

                    @Override public void onFailure(Exception pException) {
                        mFileRemoteDataSource.getSessionsList(new ILoadCallback<List<DataSession>>() {
                            @Override public void onSuccess(List<DataSession> pObject) {
                                mMemCacheDataSession = pObject;
                                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                            }

                            @Override public void onFailure(Exception pException) {
                                // Could not find data ANYWHERE
                                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
                            }
                        });
                    }
                });
            }
        });
    }


    @Background(serial = "serial")
    @Override
    public void getFavoriteSessionsList(final ILoadCallback<List<DataSession>> pLoadCallback) {
        this.getSessionsList(new ILoadCallback<List<DataSession>>() {
            @Override public void onSuccess(final List<DataSession> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                final List<DataSession> favoriteSessions = new ArrayList<>();
                for (int i = 0; i < pObject.size(); i++) {
                    final int finalI = i;
                    mLocalSnappyDataSource.isSessionFavorite(pObject.get(i).getId(), new ILoadCallback<Boolean>() {
                        @Override public void onSuccess(Boolean pIsFavorite) {
                            if (pIsFavorite) {
                                favoriteSessions.add(pObject.get(finalI));
                            }
                        }

                        @Override public void onFailure(Exception pException) {

                        }
                    });
                }
                onUiThreadCallOnSuccessCallback(pLoadCallback, favoriteSessions);
            }

            @Override public void onFailure(Exception pException) {
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    @Background(serial = "serial")
    @Override
    public void getTimeFramesList(final ILoadCallback<List<DataTimeFrame>> pLoadCallback) {
        ensureConferenceLoaded();
        if (mMemCacheTimeFrame != null) {
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheTimeFrame);
            return;
        }
        this.mLocalSnappyDataSource.getTimeFramesList(new ILoadCallback<List<DataTimeFrame>>() {
            @Override public void onSuccess(List<DataTimeFrame> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheTimeFrame = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {

            }
        });
    }

    @Background(serial = "serial")
    @Override
    public void getCodecampersList(final ILoadCallback<List<DataCodecamper>> pLoadCallback) {
        ensureConferenceLoaded();
        if (mMemCacheDataCodecampers != null) {
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataCodecampers);
            return;
        }
        this.mLocalSnappyDataSource.getCodecampersList(new ILoadCallback<List<DataCodecamper>>() {
            @Override public void onSuccess(List<DataCodecamper> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataCodecampers = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {

            }
        });
    }

    @Background(serial = "serial")
    @Override public void getSponsorsList(final ILoadCallback<List<DataSponsor>> pLoadCallback) {
        ensureConferenceLoaded();
        if (mMemCacheDataSponsors != null) {
            onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataSponsors);
            return;
        }
        mLocalSnappyDataSource.getSponsorsList(new ILoadCallback<List<DataSponsor>>() {
            @Override public void onSuccess(List<DataSponsor> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSponsors = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                getSponsorsFromRemote(new ILoadCallback<List<DataSponsor>>() {
                    @Override public void onSuccess(List<DataSponsor> pObject) {
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                    }

                    @Override public void onFailure(Exception pException) {
                        mFileRemoteDataSource.getSponsorsList(new ILoadCallback<List<DataSponsor>>() {
                            @Override public void onSuccess(List<DataSponsor> pObject) {
                                mMemCacheDataSponsors = pObject;
                                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                            }

                            @Override public void onFailure(Exception pException) {
                                // Could not find data ANYWHERE
                                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
                            }
                        });
                    }
                });
            }
        });
    }

    @Background(serial = "serial")
    @Override
    public void getConferencesList(final ILoadCallback<List<DataConference>> pLoadCallback) {
        getConferencesListSync(pLoadCallback);
    }

    public void getConferencesListSync(final ILoadCallback<List<DataConference>> pLoadCallback) {
        if (mMemCacheDataConferences != null) {
            this.onUiThreadCallOnSuccessCallback(pLoadCallback, mMemCacheDataConferences);
            return;
        }
        mLocalSnappyDataSource.getConferencesList(new ILoadCallback<List<DataConference>>() {
            @Override public void onSuccess(List<DataConference> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataConferences = pObject;
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {

                getConferencesFromRemote(new ILoadCallback<List<DataConference>>() {
                    @Override public void onSuccess(List<DataConference> pObject) {
                        onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                    }

                    @Override public void onFailure(Exception pException) {
                        mFileRemoteDataSource.getConferencesList(new ILoadCallback<List<DataConference>>() {
                            @Override public void onSuccess(List<DataConference> pObject) {
                                mMemCacheDataConferences = pObject;
                                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
                            }

                            @Override public void onFailure(Exception pException) {
                                // Could not find the data ANYWHERE
                                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
                            }
                        });
                    }
                });
            }
        });
    }

    private void getRoomsFromRemote(final ILoadCallback<List<DataRoom>> pLoadCallback) {
        ensureConferenceLoaded();
        mWebViewRemoteDataSource.getRoomsList(new ILoadCallback<List<DataRoom>>() {
            @Override public void onSuccess(List<DataRoom> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataRooms = pObject;
                mLocalSnappyDataSource.storeDataRooms(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getSessionsFromRemote(final ILoadCallback<List<DataSession>> pLoadCallback) {
        ensureConferenceLoaded();
        mWebViewRemoteDataSource.getSessionsList(new ILoadCallback<List<DataSession>>() {
            @Override public void onSuccess(List<DataSession> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSession = pObject;
                mLocalSnappyDataSource.storeDataSessions(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getTimeFramesFromRemote(final ILoadCallback<List<DataTimeFrame>> pLoadCallback) {
        ensureConferenceLoaded();
        mWebViewRemoteDataSource.getTimeFramesList(new ILoadCallback<List<DataTimeFrame>>() {
            @Override public void onSuccess(List<DataTimeFrame> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheTimeFrame = pObject;
                mLocalSnappyDataSource.storeDataTimeFrames(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getCodecampersFromRemote(final ILoadCallback<List<DataCodecamper>> pLoadCallback) {
        ensureConferenceLoaded();
        mWebViewRemoteDataSource.getCodecampersList(new ILoadCallback<List<DataCodecamper>>() {
            @Override public void onSuccess(List<DataCodecamper> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataCodecampers = pObject;
                mLocalSnappyDataSource.storeDataCodecampers(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: Can't fail more than that :))", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getSponsorsFromRemote(final ILoadCallback<List<DataSponsor>> pLoadCallback) {
        ensureConferenceLoaded();
        mWebViewRemoteDataSource.getSponsorsList(new ILoadCallback<List<DataSponsor>>() {
            @Override public void onSuccess(List<DataSponsor> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataSponsors = pObject;
                mLocalSnappyDataSource.storeDataSponsors(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: can't fail more than that :)", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    private void getConferencesFromRemote(final ILoadCallback<List<DataConference>> pLoadCallback) {
        mWebViewRemoteDataSource.getConferencesList(new ILoadCallback<List<DataConference>>() {
            @Override public void onSuccess(List<DataConference> pObject) {
                if (pObject == null) {
                    this.onFailure(new DataNotFoundException());
                    return;
                }
                mMemCacheDataConferences = pObject;
                mLocalSnappyDataSource.storeDataConferences(pObject);
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                Log.e(TAG, "onFailure: can't fail more than that :)", pException);
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    @Background(serial = "serial")
    @Override public void getRoom(Long pLong, ILoadCallback<DataRoom> pLoadCallback) {

    }

    @Background(serial = "serial")
    @Override public void getSession(Long pLong, ILoadCallback<DataSession> pLoadCallback) {

    }

    @Background(serial = "serial")
    @Override public void getTimeFrame(Long pLong, ILoadCallback<DataTimeFrame> pLoadCallback) {

    }

    @Background(serial = "serial")
    @Override public void getCodecamper(Long pLong, ILoadCallback<DataCodecamper> pLoadCallback) {

    }

    @Background(serial = "serial")
    @Override
    public void isSessionFavorite(Long pLong, final ILoadCallback<Boolean> pLoadCallback) {
        mLocalSnappyDataSource.isSessionFavorite(pLong, new ILoadCallback<Boolean>() {
            @Override public void onSuccess(Boolean pObject) {
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    @Background(serial = "serial")
    @Override
    public void setSessionFavorite(Long pLong, boolean pFavorite, final ILoadCallback<Boolean> pLoadCallback) {
        this.mLocalSnappyDataSource.setSessionFavorite(pLong, pFavorite, new ILoadCallback<Boolean>() {
            @Override public void onSuccess(Boolean pObject) {
                onUiThreadCallOnSuccessCallback(pLoadCallback, pObject);
            }

            @Override public void onFailure(Exception pException) {
                onUiThreadCallOnFailureCallback(pLoadCallback, pException);
            }
        });
    }

    public void setConference(DataConference pConference) {
        this.mLocalSnappyDataSource.setConference(pConference);
        this.mFileRemoteDataSource.setConference(pConference);
        this.mWebViewRemoteDataSource.setConference(pConference);
        this.mDataConference = pConference;
    }

    @Override public DataConference getConference() {
        return mDataConference;
    }

    @UiThread
    public <Model> void onUiThreadCallOnSuccessCallback(ILoadCallback<Model> pLoadCallback, Model pModel) {
        pLoadCallback.onSuccess(pModel);
    }

    @UiThread
    public <E extends Exception> void onUiThreadCallOnFailureCallback(ILoadCallback pLoadCallback, E pException) {
        pLoadCallback.onFailure(pException);
    }

    public void setLatestConference(List<DataConference> conferences) {
        this.setConference(DataConference.getLatestEvent(conferences));
    }

    private void invalidateDataRoomsList() {
        this.mMemCacheDataRooms = null;
        this.mLocalSnappyDataSource.invalidateDataRooms();
    }

    private void inavlidateTimeFrameList() {
        this.mMemCacheTimeFrame = null;
        this.mLocalSnappyDataSource.invalidateDataTimeFrames();
    }

    private void invalidateCodecampersList() {
        this.mMemCacheDataCodecampers = null;
        this.mLocalSnappyDataSource.invalidateDataCodecampers();
    }

    private void invalidateDataSessions() {
        this.mMemCacheDataSession = null;
        this.mLocalSnappyDataSource.invalidateDataSessions();
    }

    private void invalidateDataSponsors() {
        this.mMemCacheDataSponsors = null;
        this.mLocalSnappyDataSource.invalidateDataSponsors();
    }

    private void invalidateDataConferences() {
//        this.mMemCacheDataConferences = null;
//        this.mLocalSnappyDataSource.invalidateDataConferences();
    }

    @Override public void invalidate() {
        this.mMemCacheDataRooms = null;
        this.mMemCacheTimeFrame = null;
        this.mMemCacheDataCodecampers = null;
        this.mMemCacheDataSession = null;
        this.mMemCacheDataSponsors = null;
//        this.mMemCacheDataConferences = null;
        this.mLocalSnappyDataSource.invalidate();
    }
}
