package com.rongqi.vector.milvus;

import com.rongqi.vector.annotation.IndexType;
import com.rongqi.vector.annotation.MetricType;
import com.rongqi.vector.annotation.VectorDataType;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;

/**
 * RongQi Vector 枚举到 Milvus SDK 枚举的转换器。
 */
public class MilvusTypeMapper {

    public DataType toMilvusDataType(VectorDataType type) {
        switch (type) {
            case BOOL:
                return DataType.Bool;
            case INT8:
                return DataType.Int8;
            case INT16:
                return DataType.Int16;
            case INT32:
                return DataType.Int32;
            case INT64:
                return DataType.Int64;
            case FLOAT:
                return DataType.Float;
            case DOUBLE:
                return DataType.Double;
            case VARCHAR:
                return DataType.VarChar;
            case JSON:
                return DataType.JSON;
            case ARRAY:
                return DataType.Array;
            case FLOAT_VECTOR:
                return DataType.FloatVector;
            case BINARY_VECTOR:
                return DataType.BinaryVector;
            case SPARSE_FLOAT_VECTOR:
                return DataType.SparseFloatVector;
            default:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "不支持的 Milvus 字段类型: " + type);
        }
    }

    public IndexParam.IndexType toMilvusIndexType(IndexType type) {
        switch (type) {
            case HNSW:
                return IndexParam.IndexType.HNSW;
            case IVF_FLAT:
                return IndexParam.IndexType.IVF_FLAT;
            case IVF_SQ8:
                return IndexParam.IndexType.IVF_SQ8;
            case AUTOINDEX:
                return IndexParam.IndexType.AUTOINDEX;
            case INVERTED:
                return IndexParam.IndexType.INVERTED;
            default:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "不支持的 Milvus 索引类型: " + type);
        }
    }

    public IndexParam.MetricType toMilvusMetricType(MetricType type) {
        switch (type) {
            case COSINE:
                return IndexParam.MetricType.COSINE;
            case L2:
                return IndexParam.MetricType.L2;
            case IP:
                return IndexParam.MetricType.IP;
            default:
                throw new VectorException(VectorErrorCode.VECTOR_SCHEMA_INVALID,
                        "不支持的 Milvus metricType: " + type);
        }
    }
}
