package com.iptvcinema.tv.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import coil.ImageLoader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iptvcinema.tv.core.data.local.LocalCredentialsStore;
import com.iptvcinema.tv.core.data.repository.CatalogRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseAuthRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseDeviceActivationRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseFavoritesRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseParentalControlsRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseProfilesRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseUserSettingsRepository;
import com.iptvcinema.tv.core.data.repository.supabase.SupabaseWatchHistoryRepository;
import com.iptvcinema.tv.core.database.CatalogDaoFacade;
import com.iptvcinema.tv.core.database.IptvDatabase;
import com.iptvcinema.tv.core.database.di.DatabaseModule_ProvideIptvDatabaseFactory;
import com.iptvcinema.tv.core.datastore.AppSessionRepository;
import com.iptvcinema.tv.core.datastore.RecentSearchRepository;
import com.iptvcinema.tv.core.datastore.StartupSessionBootstrap;
import com.iptvcinema.tv.core.datastore.di.DataStoreModule_ProvideAppPreferencesDataStoreFactory;
import com.iptvcinema.tv.core.di.CoilModule_ProvideImageLoaderFactory;
import com.iptvcinema.tv.core.di.CoroutineModule_ProvideApplicationScopeFactory;
import com.iptvcinema.tv.core.epg.EpgSyncRepository;
import com.iptvcinema.tv.core.epg.XmltvParser;
import com.iptvcinema.tv.core.m3u.M3uDownloader;
import com.iptvcinema.tv.core.m3u.M3uSyncRepository;
import com.iptvcinema.tv.core.navigation.SessionViewModel;
import com.iptvcinema.tv.core.navigation.SessionViewModel_HiltModules;
import com.iptvcinema.tv.core.navigation.SessionViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.core.navigation.SessionViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.core.network.NetworkModule_ProvideOkHttpClientFactory;
import com.iptvcinema.tv.core.network.XtreamRetrofitFactory;
import com.iptvcinema.tv.core.parental.ParentalGate;
import com.iptvcinema.tv.core.parental.ParentalSession;
import com.iptvcinema.tv.core.parental.PinHasher;
import com.iptvcinema.tv.core.player.EpisodeCatalogRepository;
import com.iptvcinema.tv.core.player.NextEpisodeResolver;
import com.iptvcinema.tv.core.player.PlaybackRepository;
import com.iptvcinema.tv.core.player.PlaybackSessionTracker;
import com.iptvcinema.tv.core.player.PlayerManager;
import com.iptvcinema.tv.core.player.WatchedSeriesEpisodePrefetcher;
import com.iptvcinema.tv.core.supabase.di.SupabaseModule_ProvideHttpClientFactory;
import com.iptvcinema.tv.core.supabase.di.SupabaseModule_ProvideJsonFactory;
import com.iptvcinema.tv.core.supabase.di.SupabaseModule_ProvideSupabaseClientFactory;
import com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipher;
import com.iptvcinema.tv.core.util.AppStrings;
import com.iptvcinema.tv.core.xtream.XtreamRepository;
import com.iptvcinema.tv.core.xtream.XtreamSyncRepository;
import com.iptvcinema.tv.features.activation.ActivationViewModel;
import com.iptvcinema.tv.features.activation.ActivationViewModel_HiltModules;
import com.iptvcinema.tv.features.activation.ActivationViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.activation.ActivationViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.details.DetailsViewModel;
import com.iptvcinema.tv.features.details.DetailsViewModel_HiltModules;
import com.iptvcinema.tv.features.details.DetailsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.details.DetailsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.home.HomeViewModel;
import com.iptvcinema.tv.features.home.HomeViewModel_HiltModules;
import com.iptvcinema.tv.features.home.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.home.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.livetv.LiveTvViewModel;
import com.iptvcinema.tv.features.livetv.LiveTvViewModel_HiltModules;
import com.iptvcinema.tv.features.livetv.LiveTvViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.livetv.LiveTvViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.movies.MoviesViewModel;
import com.iptvcinema.tv.features.movies.MoviesViewModel_HiltModules;
import com.iptvcinema.tv.features.movies.MoviesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.movies.MoviesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.mylist.MyListViewModel;
import com.iptvcinema.tv.features.mylist.MyListViewModel_HiltModules;
import com.iptvcinema.tv.features.mylist.MyListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.mylist.MyListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.parental.ParentalControlsViewModel;
import com.iptvcinema.tv.features.parental.ParentalControlsViewModel_HiltModules;
import com.iptvcinema.tv.features.parental.ParentalControlsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.parental.ParentalControlsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.player.PlayerViewModel;
import com.iptvcinema.tv.features.player.PlayerViewModel_HiltModules;
import com.iptvcinema.tv.features.player.PlayerViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.player.PlayerViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.profiles.ProfileViewModel;
import com.iptvcinema.tv.features.profiles.ProfileViewModel_HiltModules;
import com.iptvcinema.tv.features.profiles.ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.profiles.ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.search.SearchViewModel;
import com.iptvcinema.tv.features.search.SearchViewModel_HiltModules;
import com.iptvcinema.tv.features.search.SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.search.SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.series.SeriesViewModel;
import com.iptvcinema.tv.features.series.SeriesViewModel_HiltModules;
import com.iptvcinema.tv.features.series.SeriesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.series.SeriesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.settings.SettingsViewModel;
import com.iptvcinema.tv.features.settings.SettingsViewModel_HiltModules;
import com.iptvcinema.tv.features.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.sources.SourceViewModel;
import com.iptvcinema.tv.features.sources.SourceViewModel_HiltModules;
import com.iptvcinema.tv.features.sources.SourceViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.sources.SourceViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.iptvcinema.tv.features.splash.SplashViewModel;
import com.iptvcinema.tv.features.splash.SplashViewModel_HiltModules;
import com.iptvcinema.tv.features.splash.SplashViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.iptvcinema.tv.features.splash.SplashViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import io.github.jan.supabase.SupabaseClient;
import io.ktor.client.HttpClient;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DaggerIptvCinemaApp_HiltComponents_SingletonC {
  private DaggerIptvCinemaApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public IptvCinemaApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements IptvCinemaApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements IptvCinemaApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements IptvCinemaApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements IptvCinemaApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements IptvCinemaApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements IptvCinemaApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements IptvCinemaApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public IptvCinemaApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends IptvCinemaApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends IptvCinemaApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends IptvCinemaApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends IptvCinemaApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(15).put(ActivationViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ActivationViewModel_HiltModules.KeyModule.provide()).put(DetailsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DetailsViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(LiveTvViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, LiveTvViewModel_HiltModules.KeyModule.provide()).put(MoviesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MoviesViewModel_HiltModules.KeyModule.provide()).put(MyListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MyListViewModel_HiltModules.KeyModule.provide()).put(ParentalControlsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ParentalControlsViewModel_HiltModules.KeyModule.provide()).put(PlayerViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, PlayerViewModel_HiltModules.KeyModule.provide()).put(ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileViewModel_HiltModules.KeyModule.provide()).put(SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SearchViewModel_HiltModules.KeyModule.provide()).put(SeriesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SeriesViewModel_HiltModules.KeyModule.provide()).put(SessionViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SessionViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(SourceViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SourceViewModel_HiltModules.KeyModule.provide()).put(SplashViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SplashViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends IptvCinemaApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<ActivationViewModel> activationViewModelProvider;

    private Provider<DetailsViewModel> detailsViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LiveTvViewModel> liveTvViewModelProvider;

    private Provider<MoviesViewModel> moviesViewModelProvider;

    private Provider<MyListViewModel> myListViewModelProvider;

    private Provider<ParentalControlsViewModel> parentalControlsViewModelProvider;

    private Provider<PlayerViewModel> playerViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<SearchViewModel> searchViewModelProvider;

    private Provider<SeriesViewModel> seriesViewModelProvider;

    private Provider<SessionViewModel> sessionViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SourceViewModel> sourceViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.activationViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.detailsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.liveTvViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.moviesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.myListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.parentalControlsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.playerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.searchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.seriesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.sessionViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.sourceViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(15).put(ActivationViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) activationViewModelProvider)).put(DetailsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) detailsViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(LiveTvViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) liveTvViewModelProvider)).put(MoviesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) moviesViewModelProvider)).put(MyListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) myListViewModelProvider)).put(ParentalControlsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) parentalControlsViewModelProvider)).put(PlayerViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) playerViewModelProvider)).put(ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) profileViewModelProvider)).put(SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) searchViewModelProvider)).put(SeriesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) seriesViewModelProvider)).put(SessionViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) sessionViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(SourceViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) sourceViewModelProvider)).put(SplashViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) splashViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.iptvcinema.tv.features.activation.ActivationViewModel 
          return (T) new ActivationViewModel(singletonCImpl.supabaseAuthRepositoryProvider.get(), singletonCImpl.supabaseDeviceActivationRepositoryProvider.get(), singletonCImpl.startupSessionBootstrapProvider.get());

          case 1: // com.iptvcinema.tv.features.details.DetailsViewModel 
          return (T) new DetailsViewModel(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseFavoritesRepositoryProvider.get(), singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.episodeCatalogRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get(), singletonCImpl.appStringsProvider.get());

          case 2: // com.iptvcinema.tv.features.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.supabaseWatchHistoryRepositoryProvider.get(), singletonCImpl.supabaseUserSettingsRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get(), singletonCImpl.playbackSessionTrackerProvider.get(), singletonCImpl.appStringsProvider.get());

          case 3: // com.iptvcinema.tv.features.livetv.LiveTvViewModel 
          return (T) new LiveTvViewModel(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get(), singletonCImpl.playbackSessionTrackerProvider.get());

          case 4: // com.iptvcinema.tv.features.movies.MoviesViewModel 
          return (T) new MoviesViewModel(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get());

          case 5: // com.iptvcinema.tv.features.mylist.MyListViewModel 
          return (T) new MyListViewModel(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.supabaseFavoritesRepositoryProvider.get(), singletonCImpl.supabaseWatchHistoryRepositoryProvider.get(), singletonCImpl.appStringsProvider.get());

          case 6: // com.iptvcinema.tv.features.parental.ParentalControlsViewModel 
          return (T) new ParentalControlsViewModel(singletonCImpl.supabaseProfilesRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.pinHasherProvider.get(), singletonCImpl.parentalGateProvider.get());

          case 7: // com.iptvcinema.tv.features.player.PlayerViewModel 
          return (T) new PlayerViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.playbackRepositoryProvider.get(), singletonCImpl.playerManagerProvider.get(), singletonCImpl.supabaseWatchHistoryRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseUserSettingsRepositoryProvider.get(), singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.nextEpisodeResolverProvider.get(), singletonCImpl.episodeCatalogRepositoryProvider.get(), singletonCImpl.playbackSessionTrackerProvider.get(), singletonCImpl.appStringsProvider.get());

          case 8: // com.iptvcinema.tv.features.profiles.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseProfilesRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get());

          case 9: // com.iptvcinema.tv.features.search.SearchViewModel 
          return (T) new SearchViewModel(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.recentSearchRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get(), singletonCImpl.provideAppPreferencesDataStoreProvider.get(), singletonCImpl.appStringsProvider.get());

          case 10: // com.iptvcinema.tv.features.series.SeriesViewModel 
          return (T) new SeriesViewModel(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get());

          case 11: // com.iptvcinema.tv.core.navigation.SessionViewModel 
          return (T) new SessionViewModel(singletonCImpl.appSessionRepositoryProvider.get());

          case 12: // com.iptvcinema.tv.features.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseAuthRepositoryProvider.get(), singletonCImpl.supabaseUserSettingsRepositoryProvider.get(), singletonCImpl.supabaseParentalControlsRepositoryProvider.get(), singletonCImpl.parentalGateProvider.get(), singletonCImpl.localCredentialsStoreProvider.get());

          case 13: // com.iptvcinema.tv.features.sources.SourceViewModel 
          return (T) new SourceViewModel(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabasePlaylistSourcesRepositoryProvider.get(), singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.supabaseAuthRepositoryProvider.get(), singletonCImpl.xtreamRepositoryProvider.get(), singletonCImpl.xtreamSyncRepositoryProvider.get(), singletonCImpl.m3uSyncRepositoryProvider.get(), singletonCImpl.localCredentialsStoreProvider.get());

          case 14: // com.iptvcinema.tv.features.splash.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.startupSessionBootstrapProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends IptvCinemaApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends IptvCinemaApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends IptvCinemaApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<ImageLoader> provideImageLoaderProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<SupabaseClient> provideSupabaseClientProvider;

    private Provider<DataStore<Preferences>> provideAppPreferencesDataStoreProvider;

    private Provider<AppSessionRepository> appSessionRepositoryProvider;

    private Provider<SupabaseAuthRepository> supabaseAuthRepositoryProvider;

    private Provider<HttpClient> provideHttpClientProvider;

    private Provider<SupabaseDeviceActivationRepository> supabaseDeviceActivationRepositoryProvider;

    private Provider<LocalCredentialsStore> localCredentialsStoreProvider;

    private Provider<CloudCredentialsCipher> cloudCredentialsCipherProvider;

    private Provider<SupabasePlaylistSourcesRepository> supabasePlaylistSourcesRepositoryProvider;

    private Provider<SupabaseWatchHistoryRepository> supabaseWatchHistoryRepositoryProvider;

    private Provider<IptvDatabase> provideIptvDatabaseProvider;

    private Provider<CatalogDaoFacade> catalogDaoFacadeProvider;

    private Provider<AppStrings> appStringsProvider;

    private Provider<CatalogRepository> catalogRepositoryProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<XtreamRetrofitFactory> xtreamRetrofitFactoryProvider;

    private Provider<XtreamRepository> xtreamRepositoryProvider;

    private Provider<EpisodeCatalogRepository> episodeCatalogRepositoryProvider;

    private Provider<WatchedSeriesEpisodePrefetcher> watchedSeriesEpisodePrefetcherProvider;

    private Provider<StartupSessionBootstrap> startupSessionBootstrapProvider;

    private Provider<SupabaseFavoritesRepository> supabaseFavoritesRepositoryProvider;

    private Provider<SupabaseParentalControlsRepository> supabaseParentalControlsRepositoryProvider;

    private Provider<PinHasher> pinHasherProvider;

    private Provider<ParentalSession> parentalSessionProvider;

    private Provider<ParentalGate> parentalGateProvider;

    private Provider<SupabaseUserSettingsRepository> supabaseUserSettingsRepositoryProvider;

    private Provider<PlaybackSessionTracker> playbackSessionTrackerProvider;

    private Provider<SupabaseProfilesRepository> supabaseProfilesRepositoryProvider;

    private Provider<PlaybackRepository> playbackRepositoryProvider;

    private Provider<PlayerManager> playerManagerProvider;

    private Provider<NextEpisodeResolver> nextEpisodeResolverProvider;

    private Provider<RecentSearchRepository> recentSearchRepositoryProvider;

    private Provider<XmltvParser> xmltvParserProvider;

    private Provider<CoroutineScope> provideApplicationScopeProvider;

    private Provider<EpgSyncRepository> epgSyncRepositoryProvider;

    private Provider<XtreamSyncRepository> xtreamSyncRepositoryProvider;

    private Provider<M3uDownloader> m3uDownloaderProvider;

    private Provider<M3uSyncRepository> m3uSyncRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);
      initialize2(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideImageLoaderProvider = DoubleCheck.provider(new SwitchingProvider<ImageLoader>(singletonCImpl, 0));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 3));
      this.provideSupabaseClientProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseClient>(singletonCImpl, 2));
      this.provideAppPreferencesDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 5));
      this.appSessionRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<AppSessionRepository>(singletonCImpl, 4));
      this.supabaseAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseAuthRepository>(singletonCImpl, 1));
      this.provideHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<HttpClient>(singletonCImpl, 7));
      this.supabaseDeviceActivationRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseDeviceActivationRepository>(singletonCImpl, 6));
      this.localCredentialsStoreProvider = DoubleCheck.provider(new SwitchingProvider<LocalCredentialsStore>(singletonCImpl, 10));
      this.cloudCredentialsCipherProvider = DoubleCheck.provider(new SwitchingProvider<CloudCredentialsCipher>(singletonCImpl, 11));
      this.supabasePlaylistSourcesRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabasePlaylistSourcesRepository>(singletonCImpl, 9));
      this.supabaseWatchHistoryRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseWatchHistoryRepository>(singletonCImpl, 13));
      this.provideIptvDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<IptvDatabase>(singletonCImpl, 17));
      this.catalogDaoFacadeProvider = DoubleCheck.provider(new SwitchingProvider<CatalogDaoFacade>(singletonCImpl, 16));
      this.appStringsProvider = DoubleCheck.provider(new SwitchingProvider<AppStrings>(singletonCImpl, 18));
      this.catalogRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CatalogRepository>(singletonCImpl, 15));
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 21));
      this.xtreamRetrofitFactoryProvider = DoubleCheck.provider(new SwitchingProvider<XtreamRetrofitFactory>(singletonCImpl, 20));
      this.xtreamRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<XtreamRepository>(singletonCImpl, 19));
      this.episodeCatalogRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<EpisodeCatalogRepository>(singletonCImpl, 14));
      this.watchedSeriesEpisodePrefetcherProvider = DoubleCheck.provider(new SwitchingProvider<WatchedSeriesEpisodePrefetcher>(singletonCImpl, 12));
      this.startupSessionBootstrapProvider = DoubleCheck.provider(new SwitchingProvider<StartupSessionBootstrap>(singletonCImpl, 8));
      this.supabaseFavoritesRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseFavoritesRepository>(singletonCImpl, 22));
      this.supabaseParentalControlsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseParentalControlsRepository>(singletonCImpl, 23));
      this.pinHasherProvider = DoubleCheck.provider(new SwitchingProvider<PinHasher>(singletonCImpl, 25));
    }

    @SuppressWarnings("unchecked")
    private void initialize2(final ApplicationContextModule applicationContextModuleParam) {
      this.parentalSessionProvider = DoubleCheck.provider(new SwitchingProvider<ParentalSession>(singletonCImpl, 26));
      this.parentalGateProvider = DoubleCheck.provider(new SwitchingProvider<ParentalGate>(singletonCImpl, 24));
      this.supabaseUserSettingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseUserSettingsRepository>(singletonCImpl, 27));
      this.playbackSessionTrackerProvider = DoubleCheck.provider(new SwitchingProvider<PlaybackSessionTracker>(singletonCImpl, 28));
      this.supabaseProfilesRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseProfilesRepository>(singletonCImpl, 29));
      this.playbackRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<PlaybackRepository>(singletonCImpl, 30));
      this.playerManagerProvider = DoubleCheck.provider(new SwitchingProvider<PlayerManager>(singletonCImpl, 31));
      this.nextEpisodeResolverProvider = DoubleCheck.provider(new SwitchingProvider<NextEpisodeResolver>(singletonCImpl, 32));
      this.recentSearchRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<RecentSearchRepository>(singletonCImpl, 33));
      this.xmltvParserProvider = DoubleCheck.provider(new SwitchingProvider<XmltvParser>(singletonCImpl, 36));
      this.provideApplicationScopeProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineScope>(singletonCImpl, 37));
      this.epgSyncRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<EpgSyncRepository>(singletonCImpl, 35));
      this.xtreamSyncRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<XtreamSyncRepository>(singletonCImpl, 34));
      this.m3uDownloaderProvider = DoubleCheck.provider(new SwitchingProvider<M3uDownloader>(singletonCImpl, 39));
      this.m3uSyncRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<M3uSyncRepository>(singletonCImpl, 38));
    }

    @Override
    public void injectIptvCinemaApp(IptvCinemaApp iptvCinemaApp) {
      injectIptvCinemaApp2(iptvCinemaApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private IptvCinemaApp injectIptvCinemaApp2(IptvCinemaApp instance) {
      IptvCinemaApp_MembersInjector.injectImageLoader(instance, provideImageLoaderProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // coil.ImageLoader 
          return (T) CoilModule_ProvideImageLoaderFactory.provideImageLoader(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseAuthRepository 
          return (T) new SupabaseAuthRepository(singletonCImpl.provideSupabaseClientProvider.get(), singletonCImpl.appSessionRepositoryProvider.get());

          case 2: // io.github.jan.supabase.SupabaseClient 
          return (T) SupabaseModule_ProvideSupabaseClientFactory.provideSupabaseClient(singletonCImpl.provideJsonProvider.get());

          case 3: // kotlinx.serialization.json.Json 
          return (T) SupabaseModule_ProvideJsonFactory.provideJson();

          case 4: // com.iptvcinema.tv.core.datastore.AppSessionRepository 
          return (T) new AppSessionRepository(singletonCImpl.provideAppPreferencesDataStoreProvider.get());

          case 5: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) DataStoreModule_ProvideAppPreferencesDataStoreFactory.provideAppPreferencesDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseDeviceActivationRepository 
          return (T) new SupabaseDeviceActivationRepository(singletonCImpl.provideSupabaseClientProvider.get(), singletonCImpl.supabaseAuthRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.provideHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 7: // io.ktor.client.HttpClient 
          return (T) SupabaseModule_ProvideHttpClientFactory.provideHttpClient(singletonCImpl.provideJsonProvider.get());

          case 8: // com.iptvcinema.tv.core.datastore.StartupSessionBootstrap 
          return (T) new StartupSessionBootstrap(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.supabaseAuthRepositoryProvider.get(), singletonCImpl.supabasePlaylistSourcesRepositoryProvider.get(), singletonCImpl.localCredentialsStoreProvider.get(), singletonCImpl.watchedSeriesEpisodePrefetcherProvider.get());

          case 9: // com.iptvcinema.tv.core.data.repository.supabase.SupabasePlaylistSourcesRepository 
          return (T) new SupabasePlaylistSourcesRepository(singletonCImpl.provideSupabaseClientProvider.get(), singletonCImpl.localCredentialsStoreProvider.get(), singletonCImpl.cloudCredentialsCipherProvider.get());

          case 10: // com.iptvcinema.tv.core.data.local.LocalCredentialsStore 
          return (T) new LocalCredentialsStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideJsonProvider.get());

          case 11: // com.iptvcinema.tv.core.supabase.security.CloudCredentialsCipher 
          return (T) new CloudCredentialsCipher(singletonCImpl.provideJsonProvider.get());

          case 12: // com.iptvcinema.tv.core.player.WatchedSeriesEpisodePrefetcher 
          return (T) new WatchedSeriesEpisodePrefetcher(singletonCImpl.supabaseWatchHistoryRepositoryProvider.get(), singletonCImpl.episodeCatalogRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get());

          case 13: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseWatchHistoryRepository 
          return (T) new SupabaseWatchHistoryRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 14: // com.iptvcinema.tv.core.player.EpisodeCatalogRepository 
          return (T) new EpisodeCatalogRepository(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.xtreamRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get());

          case 15: // com.iptvcinema.tv.core.data.repository.CatalogRepository 
          return (T) new CatalogRepository(singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.catalogDaoFacadeProvider.get(), singletonCImpl.supabasePlaylistSourcesRepositoryProvider.get(), singletonCImpl.appStringsProvider.get());

          case 16: // com.iptvcinema.tv.core.database.CatalogDaoFacade 
          return (T) new CatalogDaoFacade(singletonCImpl.provideIptvDatabaseProvider.get());

          case 17: // com.iptvcinema.tv.core.database.IptvDatabase 
          return (T) DatabaseModule_ProvideIptvDatabaseFactory.provideIptvDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 18: // com.iptvcinema.tv.core.util.AppStrings 
          return (T) new AppStrings(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 19: // com.iptvcinema.tv.core.xtream.XtreamRepository 
          return (T) new XtreamRepository(singletonCImpl.xtreamRetrofitFactoryProvider.get(), singletonCImpl.localCredentialsStoreProvider.get());

          case 20: // com.iptvcinema.tv.core.network.XtreamRetrofitFactory 
          return (T) new XtreamRetrofitFactory(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 21: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 22: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseFavoritesRepository 
          return (T) new SupabaseFavoritesRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 23: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseParentalControlsRepository 
          return (T) new SupabaseParentalControlsRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 24: // com.iptvcinema.tv.core.parental.ParentalGate 
          return (T) new ParentalGate(singletonCImpl.pinHasherProvider.get(), singletonCImpl.parentalSessionProvider.get());

          case 25: // com.iptvcinema.tv.core.parental.PinHasher 
          return (T) new PinHasher();

          case 26: // com.iptvcinema.tv.core.parental.ParentalSession 
          return (T) new ParentalSession();

          case 27: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseUserSettingsRepository 
          return (T) new SupabaseUserSettingsRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 28: // com.iptvcinema.tv.core.player.PlaybackSessionTracker 
          return (T) new PlaybackSessionTracker();

          case 29: // com.iptvcinema.tv.core.data.repository.supabase.SupabaseProfilesRepository 
          return (T) new SupabaseProfilesRepository(singletonCImpl.provideSupabaseClientProvider.get());

          case 30: // com.iptvcinema.tv.core.player.PlaybackRepository 
          return (T) new PlaybackRepository(singletonCImpl.catalogRepositoryProvider.get(), singletonCImpl.appSessionRepositoryProvider.get(), singletonCImpl.localCredentialsStoreProvider.get(), singletonCImpl.episodeCatalogRepositoryProvider.get());

          case 31: // com.iptvcinema.tv.core.player.PlayerManager 
          return (T) new PlayerManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 32: // com.iptvcinema.tv.core.player.NextEpisodeResolver 
          return (T) new NextEpisodeResolver(singletonCImpl.episodeCatalogRepositoryProvider.get());

          case 33: // com.iptvcinema.tv.core.datastore.RecentSearchRepository 
          return (T) new RecentSearchRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 34: // com.iptvcinema.tv.core.xtream.XtreamSyncRepository 
          return (T) new XtreamSyncRepository(singletonCImpl.xtreamRepositoryProvider.get(), singletonCImpl.catalogDaoFacadeProvider.get(), singletonCImpl.supabasePlaylistSourcesRepositoryProvider.get(), singletonCImpl.epgSyncRepositoryProvider.get(), singletonCImpl.watchedSeriesEpisodePrefetcherProvider.get(), singletonCImpl.episodeCatalogRepositoryProvider.get());

          case 35: // com.iptvcinema.tv.core.epg.EpgSyncRepository 
          return (T) new EpgSyncRepository(singletonCImpl.catalogDaoFacadeProvider.get(), singletonCImpl.xmltvParserProvider.get(), singletonCImpl.provideApplicationScopeProvider.get());

          case 36: // com.iptvcinema.tv.core.epg.XmltvParser 
          return (T) new XmltvParser();

          case 37: // @com.iptvcinema.tv.core.di.ApplicationScope kotlinx.coroutines.CoroutineScope 
          return (T) CoroutineModule_ProvideApplicationScopeFactory.provideApplicationScope();

          case 38: // com.iptvcinema.tv.core.m3u.M3uSyncRepository 
          return (T) new M3uSyncRepository(singletonCImpl.m3uDownloaderProvider.get(), singletonCImpl.catalogDaoFacadeProvider.get(), singletonCImpl.supabasePlaylistSourcesRepositoryProvider.get(), singletonCImpl.epgSyncRepositoryProvider.get());

          case 39: // com.iptvcinema.tv.core.m3u.M3uDownloader 
          return (T) new M3uDownloader(singletonCImpl.provideOkHttpClientProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
