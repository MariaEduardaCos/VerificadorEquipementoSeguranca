package br.unirv.capsafe.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile InferenceDao _inferenceDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `sessoes_inferencia` (`idSessao` TEXT NOT NULL, `dataHoraMs` INTEGER NOT NULL, `nomeModelo` TEXT NOT NULL, `tempoExecucaoMs` INTEGER NOT NULL, `totalObjetos` INTEGER NOT NULL, `confiancaMedia` REAL NOT NULL, `larguraPx` INTEGER NOT NULL, `alturaPx` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `uriImagem` TEXT, PRIMARY KEY(`idSessao`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `caixas_delimitadoras` (`idCaixa` INTEGER NOT NULL, `idSessao` TEXT NOT NULL, `rotuloClasse` TEXT NOT NULL, `confianca` REAL NOT NULL, `larguraPx` REAL NOT NULL, `alturaPx` REAL NOT NULL, `centroideX` REAL NOT NULL, `centroideY` REAL NOT NULL, `areaPx2` REAL NOT NULL, PRIMARY KEY(`idCaixa`, `idSessao`), FOREIGN KEY(`idSessao`) REFERENCES `sessoes_inferencia`(`idSessao`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_caixas_delimitadoras_idSessao` ON `caixas_delimitadoras` (`idSessao`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e10b02fed0820dc38b97d213b6dd61d5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `sessoes_inferencia`");
        db.execSQL("DROP TABLE IF EXISTS `caixas_delimitadoras`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSessoesInferencia = new HashMap<String, TableInfo.Column>(10);
        _columnsSessoesInferencia.put("idSessao", new TableInfo.Column("idSessao", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("dataHoraMs", new TableInfo.Column("dataHoraMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("nomeModelo", new TableInfo.Column("nomeModelo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("tempoExecucaoMs", new TableInfo.Column("tempoExecucaoMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("totalObjetos", new TableInfo.Column("totalObjetos", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("confiancaMedia", new TableInfo.Column("confiancaMedia", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("larguraPx", new TableInfo.Column("larguraPx", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("alturaPx", new TableInfo.Column("alturaPx", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("sincronizado", new TableInfo.Column("sincronizado", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSessoesInferencia.put("uriImagem", new TableInfo.Column("uriImagem", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSessoesInferencia = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSessoesInferencia = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSessoesInferencia = new TableInfo("sessoes_inferencia", _columnsSessoesInferencia, _foreignKeysSessoesInferencia, _indicesSessoesInferencia);
        final TableInfo _existingSessoesInferencia = TableInfo.read(db, "sessoes_inferencia");
        if (!_infoSessoesInferencia.equals(_existingSessoesInferencia)) {
          return new RoomOpenHelper.ValidationResult(false, "sessoes_inferencia(br.unirv.capsafe.data.local.SessaoEntity).\n"
                  + " Expected:\n" + _infoSessoesInferencia + "\n"
                  + " Found:\n" + _existingSessoesInferencia);
        }
        final HashMap<String, TableInfo.Column> _columnsCaixasDelimitadoras = new HashMap<String, TableInfo.Column>(9);
        _columnsCaixasDelimitadoras.put("idCaixa", new TableInfo.Column("idCaixa", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("idSessao", new TableInfo.Column("idSessao", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("rotuloClasse", new TableInfo.Column("rotuloClasse", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("confianca", new TableInfo.Column("confianca", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("larguraPx", new TableInfo.Column("larguraPx", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("alturaPx", new TableInfo.Column("alturaPx", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("centroideX", new TableInfo.Column("centroideX", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("centroideY", new TableInfo.Column("centroideY", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCaixasDelimitadoras.put("areaPx2", new TableInfo.Column("areaPx2", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCaixasDelimitadoras = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCaixasDelimitadoras.add(new TableInfo.ForeignKey("sessoes_inferencia", "CASCADE", "NO ACTION", Arrays.asList("idSessao"), Arrays.asList("idSessao")));
        final HashSet<TableInfo.Index> _indicesCaixasDelimitadoras = new HashSet<TableInfo.Index>(1);
        _indicesCaixasDelimitadoras.add(new TableInfo.Index("index_caixas_delimitadoras_idSessao", false, Arrays.asList("idSessao"), Arrays.asList("ASC")));
        final TableInfo _infoCaixasDelimitadoras = new TableInfo("caixas_delimitadoras", _columnsCaixasDelimitadoras, _foreignKeysCaixasDelimitadoras, _indicesCaixasDelimitadoras);
        final TableInfo _existingCaixasDelimitadoras = TableInfo.read(db, "caixas_delimitadoras");
        if (!_infoCaixasDelimitadoras.equals(_existingCaixasDelimitadoras)) {
          return new RoomOpenHelper.ValidationResult(false, "caixas_delimitadoras(br.unirv.capsafe.data.local.CaixaEntity).\n"
                  + " Expected:\n" + _infoCaixasDelimitadoras + "\n"
                  + " Found:\n" + _existingCaixasDelimitadoras);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e10b02fed0820dc38b97d213b6dd61d5", "caa2f58ea0f1148e63b38f6174f18700");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "sessoes_inferencia","caixas_delimitadoras");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `sessoes_inferencia`");
      _db.execSQL("DELETE FROM `caixas_delimitadoras`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(InferenceDao.class, InferenceDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public InferenceDao inferenceDao() {
    if (_inferenceDao != null) {
      return _inferenceDao;
    } else {
      synchronized(this) {
        if(_inferenceDao == null) {
          _inferenceDao = new InferenceDao_Impl(this);
        }
        return _inferenceDao;
      }
    }
  }
}
