package vn.vanxuan.dtms.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoThanhChuTest {
    @Test
    void docCacSoThuongGap() {
        assertThat(SoThanhChu.doc(1_500_000)).isEqualTo("Một triệu năm trăm nghìn đồng");
        assertThat(SoThanhChu.doc(15)).isEqualTo("Mười lăm đồng");
        assertThat(SoThanhChu.doc(21)).isEqualTo("Hai mươi mốt đồng");
        assertThat(SoThanhChu.doc(105)).isEqualTo("Một trăm lẻ năm đồng");
        assertThat(SoThanhChu.doc(1_005_000)).isEqualTo("Một triệu không trăm lẻ năm nghìn đồng");
        assertThat(SoThanhChu.doc(24_000)).isEqualTo("Hai mươi tư nghìn đồng");
    }
}
