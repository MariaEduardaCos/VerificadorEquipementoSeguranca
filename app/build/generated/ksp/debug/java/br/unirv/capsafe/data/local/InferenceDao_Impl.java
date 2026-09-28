package br.unirv.capsafe.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class InferenceDao_Impl implements InferenceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SessaoEntity> __insertionAdapterOfSessaoEntity;

  private final EntityInsertionAdapter<CaixaEntity> __insertionAdapterOfCaixaEntity;

  private final SharedSQLiteStatement __preparedStmtOfAtualizarSincronizacao;

  public InferenceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSessaoEntity = new EntityInsertionAdapter<SessaoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sessoes_inferencia` (`idSessao`,`dataHoraMs`,`nomeModelo`,`tempoExecucaoMs`,`totalObjetos`,`confiancaMedia`,`larguraPx`,`alturaPx`,`sincronizado`,`uriImagem`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SessaoEntity entity) {
        statement.bindString(1, entity.getIdSessao());
        statement.bindLong(2, entity.getDataHoraMs());
        statement.bindString(3, entity.getNomeModelo());
        statement.bindLong(4, entity.getTempoExecucaoMs());
        statement.bindLong(5, entity.getTotalObjetos());
        statement.bindDouble(6, entity.getConfiancaMedia());
        statement.bindLong(7, entity.getLarguraPx());
        statement.bindLong(8, entity.getAlturaPx());
        final int _tmp = entity.getSincronizado() ? 1 : 0;
        statement.bindLong(9, _tmp);
        if (entity.getUriImagem() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getUriImagem());
        }
      }
    };
    this.__insertionAdapterOfCaixaEntity = new EntityInsertionAdapter<CaixaEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `caixas_delimitadoras` (`idCaixa`,`idSessao`,`rotuloClasse`,`confianca`,`larguraPx`,`alturaPx`,`centroideX`,`centroideY`,`areaPx2`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CaixaEntity entity) {
        statement.bindLong(1, entity.getIdCaixa());
        statement.bindString(2, entity.getIdSessao());
        statement.bindString(3, entity.getRotuloClasse());
        statement.bindDouble(4, entity.getConfianca());
        statement.bindDouble(5, entity.getLarguraPx());
        statement.bindDouble(6, entity.getAlturaPx());
        statement.bindDouble(7, entity.getCentroideX());
        statement.bindDouble(8, entity.getCentroideY());
        statement.bindDouble(9, entity.getAreaPx2());
      }
    };
    this.__preparedStmtOfAtualizarSincronizacao = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE sessoes_inferencia SET sincronizado = ? WHERE idSessao = ?";
        return _query;
      }
    };
  }

  @Override
  public Object inserirSessao(final SessaoEntity sessao,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSessaoEntity.insert(sessao);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object inserirCaixas(final List<CaixaEntity> caixas,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCaixaEntity.insert(caixas);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object inserirSessaoComCaixas(final SessaoEntity sessao, final List<CaixaEntity> caixas,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> InferenceDao.DefaultImpls.inserirSessaoComCaixas(InferenceDao_Impl.this, sessao, caixas, __cont), $completion);
  }

  @Override
  public Object atualizarSincronizacao(final String id, final boolean sincronizado,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAtualizarSincronizacao.acquire();
        int _argIndex = 1;
        final int _tmp = sincronizado ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfAtualizarSincronizacao.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SessaoEntity>> observarHistorico() {
    final String _sql = "SELECT * FROM sessoes_inferencia ORDER BY dataHoraMs DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessoes_inferencia"}, new Callable<List<SessaoEntity>>() {
      @Override
      @NonNull
      public List<SessaoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdSessao = CursorUtil.getColumnIndexOrThrow(_cursor, "idSessao");
          final int _cursorIndexOfDataHoraMs = CursorUtil.getColumnIndexOrThrow(_cursor, "dataHoraMs");
          final int _cursorIndexOfNomeModelo = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeModelo");
          final int _cursorIndexOfTempoExecucaoMs = CursorUtil.getColumnIndexOrThrow(_cursor, "tempoExecucaoMs");
          final int _cursorIndexOfTotalObjetos = CursorUtil.getColumnIndexOrThrow(_cursor, "totalObjetos");
          final int _cursorIndexOfConfiancaMedia = CursorUtil.getColumnIndexOrThrow(_cursor, "confiancaMedia");
          final int _cursorIndexOfLarguraPx = CursorUtil.getColumnIndexOrThrow(_cursor, "larguraPx");
          final int _cursorIndexOfAlturaPx = CursorUtil.getColumnIndexOrThrow(_cursor, "alturaPx");
          final int _cursorIndexOfSincronizado = CursorUtil.getColumnIndexOrThrow(_cursor, "sincronizado");
          final int _cursorIndexOfUriImagem = CursorUtil.getColumnIndexOrThrow(_cursor, "uriImagem");
          final List<SessaoEntity> _result = new ArrayList<SessaoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SessaoEntity _item;
            final String _tmpIdSessao;
            _tmpIdSessao = _cursor.getString(_cursorIndexOfIdSessao);
            final long _tmpDataHoraMs;
            _tmpDataHoraMs = _cursor.getLong(_cursorIndexOfDataHoraMs);
            final String _tmpNomeModelo;
            _tmpNomeModelo = _cursor.getString(_cursorIndexOfNomeModelo);
            final long _tmpTempoExecucaoMs;
            _tmpTempoExecucaoMs = _cursor.getLong(_cursorIndexOfTempoExecucaoMs);
            final int _tmpTotalObjetos;
            _tmpTotalObjetos = _cursor.getInt(_cursorIndexOfTotalObjetos);
            final float _tmpConfiancaMedia;
            _tmpConfiancaMedia = _cursor.getFloat(_cursorIndexOfConfiancaMedia);
            final int _tmpLarguraPx;
            _tmpLarguraPx = _cursor.getInt(_cursorIndexOfLarguraPx);
            final int _tmpAlturaPx;
            _tmpAlturaPx = _cursor.getInt(_cursorIndexOfAlturaPx);
            final boolean _tmpSincronizado;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSincronizado);
            _tmpSincronizado = _tmp != 0;
            final String _tmpUriImagem;
            if (_cursor.isNull(_cursorIndexOfUriImagem)) {
              _tmpUriImagem = null;
            } else {
              _tmpUriImagem = _cursor.getString(_cursorIndexOfUriImagem);
            }
            _item = new SessaoEntity(_tmpIdSessao,_tmpDataHoraMs,_tmpNomeModelo,_tmpTempoExecucaoMs,_tmpTotalObjetos,_tmpConfiancaMedia,_tmpLarguraPx,_tmpAlturaPx,_tmpSincronizado,_tmpUriImagem);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object sessaoPorId(final String id, final Continuation<? super SessaoEntity> $completion) {
    final String _sql = "SELECT * FROM sessoes_inferencia WHERE idSessao = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SessaoEntity>() {
      @Override
      @Nullable
      public SessaoEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdSessao = CursorUtil.getColumnIndexOrThrow(_cursor, "idSessao");
          final int _cursorIndexOfDataHoraMs = CursorUtil.getColumnIndexOrThrow(_cursor, "dataHoraMs");
          final int _cursorIndexOfNomeModelo = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeModelo");
          final int _cursorIndexOfTempoExecucaoMs = CursorUtil.getColumnIndexOrThrow(_cursor, "tempoExecucaoMs");
          final int _cursorIndexOfTotalObjetos = CursorUtil.getColumnIndexOrThrow(_cursor, "totalObjetos");
          final int _cursorIndexOfConfiancaMedia = CursorUtil.getColumnIndexOrThrow(_cursor, "confiancaMedia");
          final int _cursorIndexOfLarguraPx = CursorUtil.getColumnIndexOrThrow(_cursor, "larguraPx");
          final int _cursorIndexOfAlturaPx = CursorUtil.getColumnIndexOrThrow(_cursor, "alturaPx");
          final int _cursorIndexOfSincronizado = CursorUtil.getColumnIndexOrThrow(_cursor, "sincronizado");
          final int _cursorIndexOfUriImagem = CursorUtil.getColumnIndexOrThrow(_cursor, "uriImagem");
          final SessaoEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpIdSessao;
            _tmpIdSessao = _cursor.getString(_cursorIndexOfIdSessao);
            final long _tmpDataHoraMs;
            _tmpDataHoraMs = _cursor.getLong(_cursorIndexOfDataHoraMs);
            final String _tmpNomeModelo;
            _tmpNomeModelo = _cursor.getString(_cursorIndexOfNomeModelo);
            final long _tmpTempoExecucaoMs;
            _tmpTempoExecucaoMs = _cursor.getLong(_cursorIndexOfTempoExecucaoMs);
            final int _tmpTotalObjetos;
            _tmpTotalObjetos = _cursor.getInt(_cursorIndexOfTotalObjetos);
            final float _tmpConfiancaMedia;
            _tmpConfiancaMedia = _cursor.getFloat(_cursorIndexOfConfiancaMedia);
            final int _tmpLarguraPx;
            _tmpLarguraPx = _cursor.getInt(_cursorIndexOfLarguraPx);
            final int _tmpAlturaPx;
            _tmpAlturaPx = _cursor.getInt(_cursorIndexOfAlturaPx);
            final boolean _tmpSincronizado;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSincronizado);
            _tmpSincronizado = _tmp != 0;
            final String _tmpUriImagem;
            if (_cursor.isNull(_cursorIndexOfUriImagem)) {
              _tmpUriImagem = null;
            } else {
              _tmpUriImagem = _cursor.getString(_cursorIndexOfUriImagem);
            }
            _result = new SessaoEntity(_tmpIdSessao,_tmpDataHoraMs,_tmpNomeModelo,_tmpTempoExecucaoMs,_tmpTotalObjetos,_tmpConfiancaMedia,_tmpLarguraPx,_tmpAlturaPx,_tmpSincronizado,_tmpUriImagem);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object caixasDaSessao(final String id,
      final Continuation<? super List<CaixaEntity>> $completion) {
    final String _sql = "SELECT * FROM caixas_delimitadoras WHERE idSessao = ? ORDER BY idCaixa ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CaixaEntity>>() {
      @Override
      @NonNull
      public List<CaixaEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdCaixa = CursorUtil.getColumnIndexOrThrow(_cursor, "idCaixa");
          final int _cursorIndexOfIdSessao = CursorUtil.getColumnIndexOrThrow(_cursor, "idSessao");
          final int _cursorIndexOfRotuloClasse = CursorUtil.getColumnIndexOrThrow(_cursor, "rotuloClasse");
          final int _cursorIndexOfConfianca = CursorUtil.getColumnIndexOrThrow(_cursor, "confianca");
          final int _cursorIndexOfLarguraPx = CursorUtil.getColumnIndexOrThrow(_cursor, "larguraPx");
          final int _cursorIndexOfAlturaPx = CursorUtil.getColumnIndexOrThrow(_cursor, "alturaPx");
          final int _cursorIndexOfCentroideX = CursorUtil.getColumnIndexOrThrow(_cursor, "centroideX");
          final int _cursorIndexOfCentroideY = CursorUtil.getColumnIndexOrThrow(_cursor, "centroideY");
          final int _cursorIndexOfAreaPx2 = CursorUtil.getColumnIndexOrThrow(_cursor, "areaPx2");
          final List<CaixaEntity> _result = new ArrayList<CaixaEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CaixaEntity _item;
            final int _tmpIdCaixa;
            _tmpIdCaixa = _cursor.getInt(_cursorIndexOfIdCaixa);
            final String _tmpIdSessao;
            _tmpIdSessao = _cursor.getString(_cursorIndexOfIdSessao);
            final String _tmpRotuloClasse;
            _tmpRotuloClasse = _cursor.getString(_cursorIndexOfRotuloClasse);
            final float _tmpConfianca;
            _tmpConfianca = _cursor.getFloat(_cursorIndexOfConfianca);
            final float _tmpLarguraPx;
            _tmpLarguraPx = _cursor.getFloat(_cursorIndexOfLarguraPx);
            final float _tmpAlturaPx;
            _tmpAlturaPx = _cursor.getFloat(_cursorIndexOfAlturaPx);
            final float _tmpCentroideX;
            _tmpCentroideX = _cursor.getFloat(_cursorIndexOfCentroideX);
            final float _tmpCentroideY;
            _tmpCentroideY = _cursor.getFloat(_cursorIndexOfCentroideY);
            final float _tmpAreaPx2;
            _tmpAreaPx2 = _cursor.getFloat(_cursorIndexOfAreaPx2);
            _item = new CaixaEntity(_tmpIdCaixa,_tmpIdSessao,_tmpRotuloClasse,_tmpConfianca,_tmpLarguraPx,_tmpAlturaPx,_tmpCentroideX,_tmpCentroideY,_tmpAreaPx2);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object sessoesNaoSincronizadas(
      final Continuation<? super List<SessaoEntity>> $completion) {
    final String _sql = "SELECT * FROM sessoes_inferencia WHERE sincronizado = 0 ORDER BY dataHoraMs ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SessaoEntity>>() {
      @Override
      @NonNull
      public List<SessaoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIdSessao = CursorUtil.getColumnIndexOrThrow(_cursor, "idSessao");
          final int _cursorIndexOfDataHoraMs = CursorUtil.getColumnIndexOrThrow(_cursor, "dataHoraMs");
          final int _cursorIndexOfNomeModelo = CursorUtil.getColumnIndexOrThrow(_cursor, "nomeModelo");
          final int _cursorIndexOfTempoExecucaoMs = CursorUtil.getColumnIndexOrThrow(_cursor, "tempoExecucaoMs");
          final int _cursorIndexOfTotalObjetos = CursorUtil.getColumnIndexOrThrow(_cursor, "totalObjetos");
          final int _cursorIndexOfConfiancaMedia = CursorUtil.getColumnIndexOrThrow(_cursor, "confiancaMedia");
          final int _cursorIndexOfLarguraPx = CursorUtil.getColumnIndexOrThrow(_cursor, "larguraPx");
          final int _cursorIndexOfAlturaPx = CursorUtil.getColumnIndexOrThrow(_cursor, "alturaPx");
          final int _cursorIndexOfSincronizado = CursorUtil.getColumnIndexOrThrow(_cursor, "sincronizado");
          final int _cursorIndexOfUriImagem = CursorUtil.getColumnIndexOrThrow(_cursor, "uriImagem");
          final List<SessaoEntity> _result = new ArrayList<SessaoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SessaoEntity _item;
            final String _tmpIdSessao;
            _tmpIdSessao = _cursor.getString(_cursorIndexOfIdSessao);
            final long _tmpDataHoraMs;
            _tmpDataHoraMs = _cursor.getLong(_cursorIndexOfDataHoraMs);
            final String _tmpNomeModelo;
            _tmpNomeModelo = _cursor.getString(_cursorIndexOfNomeModelo);
            final long _tmpTempoExecucaoMs;
            _tmpTempoExecucaoMs = _cursor.getLong(_cursorIndexOfTempoExecucaoMs);
            final int _tmpTotalObjetos;
            _tmpTotalObjetos = _cursor.getInt(_cursorIndexOfTotalObjetos);
            final float _tmpConfiancaMedia;
            _tmpConfiancaMedia = _cursor.getFloat(_cursorIndexOfConfiancaMedia);
            final int _tmpLarguraPx;
            _tmpLarguraPx = _cursor.getInt(_cursorIndexOfLarguraPx);
            final int _tmpAlturaPx;
            _tmpAlturaPx = _cursor.getInt(_cursorIndexOfAlturaPx);
            final boolean _tmpSincronizado;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSincronizado);
            _tmpSincronizado = _tmp != 0;
            final String _tmpUriImagem;
            if (_cursor.isNull(_cursorIndexOfUriImagem)) {
              _tmpUriImagem = null;
            } else {
              _tmpUriImagem = _cursor.getString(_cursorIndexOfUriImagem);
            }
            _item = new SessaoEntity(_tmpIdSessao,_tmpDataHoraMs,_tmpNomeModelo,_tmpTempoExecucaoMs,_tmpTotalObjetos,_tmpConfiancaMedia,_tmpLarguraPx,_tmpAlturaPx,_tmpSincronizado,_tmpUriImagem);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
