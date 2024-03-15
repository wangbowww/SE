//! luozi modified at 12点07分
#include <bits/stdc++.h>
using namespace std;
#define int long long

signed main()
{
    vector<int>ps(2e6,0);
    int n,m;
    cin >> n >> m;
    for(int i=0;i<n;++i){
        int t,a,b;
        cin >> t >> a >> b;
        int x = 1000 * (a-1) + b;
        if(t){
            ps[x] = 0;
        }else{
            ps[x] = 1;
        }
    }
    set<int>s;
    for(int i=0;i<m;++i){
        int x;
        cin >> x;
        s.insert(x);
    }
    vector<int>a;
    for(auto x:s){
        a.push_back(x);
    }
    n = a.size();
    int ans=0;
    vector<bool> vis(2e6,false);
    for(int i=0;i<(1<<n);++i){
        int tmp=0;
        for(int j=0;j<n;++j){
            if(i&(1<<j)){
                tmp+=a[j];
            }
        }
        if(ps[tmp] && !vis[tmp]) {
            ans++;
            vis[tmp] = 1;
        }
    }
    cout << ans << "\n";
}