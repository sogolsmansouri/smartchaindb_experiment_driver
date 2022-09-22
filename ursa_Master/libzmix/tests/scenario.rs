extern crate amcl_wrapper;
extern crate zmix;
extern crate ursa;


use amcl_wrapper::group_elem::GroupElement;
use zmix::signatures::prelude::*;
use zmix::signatures::ps::prelude::*;


use amcl_wrapper::field_elem::{FieldElement, FieldElementVector};

use std::hash::{Hash, Hasher};
use std::collections::hash_map::DefaultHasher;

use amcl_wrapper::extension_field_gt::GT;



use std::mem::transmute;
use std::env;

use serde::Deserialize;

// use std::io::{stdin,stdout,Write};

// use amcl_wrapper::ECCurve::big::BIG;

// use ursa::cl::issuer::Issuer;
// use ursa::cl::prover::Prover;
// use ursa::cl::verifier::Verifier;
// use ursa::cl::*;
// use std::time::{Duration, Instant};

//Create gpk, gmsk
pub fn GSetup (count_msgs: usize, label: &[u8])->(Gpk, Gmsk){
    println!("GSetup Start.........");
    let (gpk, gmsk) = For_GSetup(count_msgs, label);
    // print_type_of(&gpk);
    println!("GSetup Successful!");
    (gpk, gmsk)
}

//Create usk[i] and upk[i] for Gjoin 
pub fn PKIJoin (count_msgs: usize, label: &[u8])->(PublicKey,SecretKey){
    println!("PKIJoin Start.........");
    let (upk_i, usk_i) = keygen(count_msgs, label);
    let msg = FieldElementVector::random(count_msgs);
    let sign_usk_i=Signature::new(msg.as_slice(), &usk_i, &upk_i).unwrap();
    // let check=sign_usk_i.verify(msg.as_slice(),&upk_i).unwrap();
    // println!("usk_i, upk_i pair checks out: {}",check);
    println!("PKIJoin Successful!");
    (upk_i, usk_i)
}

//Need to convert G1 into number so it can be signed
pub fn hashing(s: DefaultHasher,message: amcl_wrapper::group_elem_g1::G1)->u64{
    let mut hasher = s.clone();
    message.hash(&mut hasher);
    hasher.finish()
}
// Need this so tow can be a FieldElementVector
pub fn sign_usk_i(s:DefaultHasher,tow:amcl_wrapper::group_elem_g1::G1,usk_i:SecretKey, upk_i:PublicKey)->Signature{
    let tow_hash=hashing(s.clone(),tow.clone()).to_be_bytes();
    // println!("{:?}", tow_hash);
    let oneMess = FieldElement::from_msg_hash(&tow_hash);
    let mut msg=FieldElementVector::new(0);
    // println!("{:?}", tow_hash % 20 );
    msg.push(oneMess);
    // println!("{:?}", msg);
    Signature::new(msg.as_slice(), &usk_i, &upk_i).unwrap()

}
// Check sign_usk_i signature
pub fn verify_usk_i(signature_usk_i: Signature,s:DefaultHasher,tow:amcl_wrapper::group_elem_g1::G1, upk_i:PublicKey)->bool{

    let tow_hash=hashing(s.clone(),tow.clone()).to_be_bytes();
    let oneMess = FieldElement::from_msg_hash(&tow_hash);
    let mut msg=FieldElementVector::new(0);
    msg.push(oneMess);
    // println!("{:?}", msg);
    let check=signature_usk_i.verify(msg.as_slice(),&upk_i).unwrap();
    check
}

//using interactive sigma protocol, when ski is the only thing given
pub fn test_sigmaProtocol(g:amcl_wrapper::group_elem_g1::G1,y:FieldElement,Y:amcl_wrapper::group_elem_g1::G1)->(){
    //Proofer/USER calculate r and A
    let r = FieldElement::random();
    let A=&g*&r;
    //Proofer send A to Verifer
    //Verifer/GROUP MANAGER Calculate cha
    let cha = FieldElement::random();
    //Verifer send cha to Proofer
    //Proofer calculate rsp
    let rsp=&r-&y*&cha;
    //Proofer send rsp to Verifer
    // Verifer check if A=g^rsp*Y^cha
    let Check=&g*&rsp+&Y*&cha;
    println!("Proof of USER knowing ski: {:?}", A==Check);

}

//Without this the requester cannot sign and if there’s no signature then there’s nothing to verify
pub fn GJoin (i: usize, gpk: Gpk,gmsk: Gmsk, upk_i:PublicKey ,usk_i:SecretKey)->((usize,amcl_wrapper::group_elem_g1::G1,Signature,amcl_wrapper::group_elem_g2::G2,DefaultHasher), (amcl_wrapper::field_elem::FieldElement, (amcl_wrapper::group_elem_g1::G1, amcl_wrapper::group_elem_g1::G1), amcl_wrapper::extension_field_gt::GT)){
    println!("GJoin Start.........");
    //USER generates a secret key,τ, τ_tidle, η and send τ, τ_tidle and η
    println!("USER create ski, τ, τ_tidle and η and send τ, τ_tidle and η");
    let ski= FieldElement::random();
    let tow=&gpk.g * &ski;
    let tow_tilde= &gpk.Y_tilde * &ski;
    let mut hash_saved = DefaultHasher::new();
    let n =sign_usk_i(hash_saved.clone(), tow.clone(), usk_i.clone(), upk_i.clone());
    // let m =sign_usk_i(s.clone(), tow.clone(), usk_i.clone(), upk_i.clone());
    // let check1=verify_usk_i(n.clone(),s.clone(), tow.clone(),upk_i.clone());
    // let check2=verify_usk_i(m.clone(),s.clone(), tow.clone(),upk_i.clone());
    // println!("{:?}",check1);
    // println!("{:?}",check2);
    

    //GROUP MANAGER tests e(τ, Y_tilde) =e(g, τ_tilde)
    let res = GT::ate_pairing(&tow, &gpk.Y_tilde);
    let res2 = GT::ate_pairing(&gpk.g, &tow_tilde);
    println!("GROUP MANAGER tests e(τ, Y_tilde) =e(g, τ_tilde): {:?}", res==res2);
    // println!("{:?}", res==res2);


    println!("USER Start Proof of knowledge of ski");
    //User start proof of knowledge for ski
    // let pk=(&tow, &gpk.Y_tilde);
    // test_PoK_multiple_sigs(pk,ski);
    test_sigmaProtocol(gpk.g.clone(),ski.clone(),tow.clone());
    

    println!("Group Manager Generates u, σ");
    //Group MANAGER u, σ←(σ1,σ2)←(gu,(gx·(τ)y)u) 
    let u= FieldElement::random();
    let sigma1=&gpk.g * &u;
    //(g^x·(τ)^y)^u=g^x^u·(τ)^y^u IS this true?????
    let sigma2=&gpk.g * &gmsk.x * &u + &tow * &gmsk.y * &u;
    let sigma=(sigma1.clone(),sigma2.clone());


    println!("Group Manager Stores i,τ,η,τ_tilde and hash");
    //Group Manager Store (i,τ,η,τ_tilde) need to add s for hasher
    let secret_register=(i,tow,n,tow_tilde,hash_saved);

    println!("USER Stores ski,σ,e(σ1,Y_tilde)");
    //User Store (ski,σ,e(σ1,Y_tilde))
    let gsk_i=(ski,sigma,GT::ate_pairing(&sigma1,&gpk.Y_tilde));

    println!("GJoin Successful!");

    (secret_register,gsk_i)

}




//Hash tuple of messy G1,GT,str
pub fn H1(s:DefaultHasher,message:(amcl_wrapper::group_elem_g1::G1,
    amcl_wrapper::group_elem_g1::G1,
    amcl_wrapper::extension_field_gt::GT,&str))->u64{
    let mut hasher = s.clone();
    message.hash(&mut hasher);
    hasher.finish()
}

// Requester sign message with ski[i] and outputs signature and message
pub fn GSign(gsk_i:(amcl_wrapper::field_elem::FieldElement, 
    (amcl_wrapper::group_elem_g1::G1, 
        amcl_wrapper::group_elem_g1::G1),
    amcl_wrapper::extension_field_gt::GT),msg:&'static  str)->(
    (amcl_wrapper::group_elem_g1::G1, 
    amcl_wrapper::group_elem_g1::G1, 
    amcl_wrapper::field_elem::FieldElement, 
    amcl_wrapper::field_elem::FieldElement), 
    DefaultHasher,&'static  str){
    println!("GSign Start.........");
    // let msg="test_message";
    let ski=gsk_i.0;
    let sigma1=gsk_i.1.0;
    let sigma2=gsk_i.1.1;
    let e=gsk_i.2;

    //USER Create t and  computing  (σ′1,σ′2)←(σt1,σt2)
    let t = FieldElement::random();
    let sigma1_dash=sigma1 * &t;
    let sigma2_dash=sigma2 * &t;

    //USER create a  signature  of  knowledge  ofski.
    let k = FieldElement::random();
    // e(σ′1, Y_tilde)^k←e(σ1, Y_tilde)^k·t
    let e_tok_tot=e.pow(&k).pow(&t);

    //Please note code need to convert (σ′1,σ′2,e(σ1, Y_tilde)^k·t,m) to a hash u8 so this tuple can be converted into Fieldelement form using from_msg_hash
    let mut hash_saved = DefaultHasher::new();
    let number = H1(hash_saved.clone(),(sigma1_dash.clone(),sigma2_dash.clone(),e_tok_tot.clone(),msg)).to_be_bytes();
    // let number = let bytes: [u8; 4] = unsafe { transmute(H1((sigma1_dash.clone(),sigma2_dash.clone(),e_tok_tot.clone(),msg)).to_be()) };
    // let number2 = H1(ss.clone(),(sigma1_dash.clone(),sigma2_dash.clone(),e_tok_tot.clone(),msg)).to_be_bytes();
    // println!("{:?}", number);

    //c needs to be a fieldElement
    let c = FieldElement::from_msg_hash(&number);
    // //make sure hash consistent
    // let c2 = FieldElement::from_msg_hash(&number);
    // println!("{:?}", c);
    // println!("{:?}", c2);

    // USER Compute s←k+c·ski
    let s = &k + &c * &ski;

    //Output outputs (σ′1,σ′2,c,s) and m
    let mu=(sigma1_dash,sigma2_dash,c,s);
    println!("GSign Successful!");

    (mu,hash_saved.clone(), msg)

}


//Verify Requester Group ID
pub fn GVerify(gpk: Gpk,mu:(amcl_wrapper::group_elem_g1::G1, 
    amcl_wrapper::group_elem_g1::G1, 
    amcl_wrapper::field_elem::FieldElement, 
    amcl_wrapper::field_elem::FieldElement), 
    hash_for_tuple:DefaultHasher,msg:&'static  str)->bool{

    println!("GVerify Start.........");
    let sigma1_dash=mu.0;
    let sigma2_dash=mu.1;
    let c=mu.2;
    let c1=c.clone();
    let s=mu.3;

    // Verifier computes R←(e(σ1^-1, X_tilde)·e(σ2, g_tilde))−c·e(σs1, Y_tilde) 
    // let b = &-c; //also works, but slower?
    let b =&c.negation();
    //Assuming (e(g1,g2)*e(h1,h2))^-c ==e(g1^-c,g2)*e(h1^-c,h2)
    let R =GT::ate_multi_pairing(vec![(&(-&sigma1_dash).scalar_mul_variable_time(b),&gpk.X_tilde),
        (&sigma2_dash.scalar_mul_variable_time(b),&gpk.g_tilde),
        (&sigma1_dash.scalar_mul_variable_time(&s),&gpk.Y_tilde)]);
    let number = H1(hash_for_tuple.clone(),(sigma1_dash.clone(),sigma2_dash.clone(),R.clone(),msg)).to_be_bytes();
    let c2 = FieldElement::from_msg_hash(&number);
    // Verify that c=H(σ1,σ2,R,m);
    println!("Does this Verify: {:?}", c1==c2);

    println!("GVerify Successful!");
    c1==c2


    // a=e(σ1^-1, X_tilde)·e(σ2, g_tilde))^−c

    // let a = GT::ate_2_pairing(&(-&sigma1_dash),&gpk.X_tilde,&sigma2_dash,&gpk.g_tilde).pow(&-c);
    // // b=e(σ1^s, Y_tilde)=e(σ1, Y_tilde)^s;
    // // let b = GT::ate_pairing(&sigma1_dash,&gpk.Y_tilde).pow(&s);
    // let b=GT::ate_pairing(&sigma1_dash.scalar_mul_variable_time(&s),&gpk.Y_tilde);
    // // R=a·b
    // // let R = a*b;

    // let b = &-c;


    // let e_vector = Vec::new();

    // let sig1_inverse=-&sigma1_dash;
    // e_vector.push((sig1_inverse,gpk.X_tilde));

    // e_vector.push((sigma2_dash,gpk.g_tilde));

    // let sig1_to_s=sigma1_dash.scalar_mul_variable_time(&s);
    // e_vector.push((sig1_to_s,gpk.Y_tilde));


    // let a1 = GT::ate_2_pairing(&(-&sigma1_dash),&gpk.X_tilde,&sigma2_dash,&gpk.g_tilde).pow(&s);
    // let a2 = GT::ate_2_pairing(&(-&sigma1_dash).scalar_mul_variable_time(&s),&gpk.X_tilde,&sigma2_dash.scalar_mul_variable_time(&s),&gpk.g_tilde);
    // println!("{:?}", a1==a2);

    //e(g1,g2)^s=e(g1^s,g2);
    // let b2=GT::ate_pairing(&sigma1_dash.scalar_mul_variable_time(&s),&gpk.Y_tilde);
    // println!("{:?}", b==b2);



    // let test1=GT::ate_pairing(&(-&sigma1_dash),&gpk.Y_tilde);
    // let test1_1=GT::ate_pairing(&sigma1_dash,&gpk.Y_tilde);
    // let test2=GT::ate_pairing(&sigma1_dash,&gpk.Y_tilde).inverse();

    // println!("{:?}", test1==test2);
    // println!("{:?}", test1_1==test2);

    // let r=ate_pairing();

}


//Used as last resort to find identity, Note need to know gpk since need g.tilde and X_tilde
pub fn GOpen(gpk: Gpk,gmsk_array: Vec<(usize,amcl_wrapper::group_elem_g1::G1,Signature,amcl_wrapper::group_elem_g2::G2,DefaultHasher)>, mu:(amcl_wrapper::group_elem_g1::G1, 
    amcl_wrapper::group_elem_g1::G1, 
    amcl_wrapper::field_elem::FieldElement, 
    amcl_wrapper::field_elem::FieldElement), 
    hash_for_tuple:DefaultHasher,msg:&'static  str)->(){

    let sigma1_dash=mu.0;
    let sigma2_dash=mu.1;
    let c=mu.2;
    let s=mu.3;
    // let mut true_tow_tilde: amcl_wrapper::group_elem_g2::G2;
    // let mut true_identity: (usize,amcl_wrapper::group_elem_g1::G1,Signature);

    //loop to find the user
    for gmsk in gmsk_array{
        let idenity_id= gmsk.0;
        let tow = gmsk.1;
        let n = gmsk.2;
        let tow_tilde = gmsk.3;
        let hash_saved = gmsk.4;

        //check e(σ2, g_tilde)·e(σ1, X_tilde)^−1=e(σ1, τ_tilde)
        if GT::ate_2_pairing(&sigma2_dash,&gpk.g_tilde,&(-&sigma1_dash),&gpk.X_tilde)==GT::ate_pairing(&sigma1_dash,&tow_tilde){
            println!("The identity is User {:?}", idenity_id);
            let true_tow_tilde=tow_tilde;
            let true_identity=(idenity_id,tow,n);

            //Proof of knowledge of τ_tilde
            //GM informs all to chanellege it's knowledge of τ_tilde
            //Verifer generates r and A
            let r = FieldElement::random();
            let cha = &gpk.g*&r;
            //Verifer sends cha to Proofer/GM, GM calculates rsp=e(A,τ_tilde)
            let rsp = GT::ate_pairing(&cha,&true_tow_tilde);
            //GM sends rsp to Verifer
            //Verifer calculates e(τ,Y_tilde)^r and check if rsp=e(τ,Y_tilde)^r
            println!("Proof of knowledge of τ_tilde {:?}", rsp==GT::ate_pairing(&true_identity.1,&gpk.Y_tilde).pow(&r));


        }
    }

}




// fn input_user()->(std::string::String){
//     use std::io::{stdin,stdout,Write};
//     let mut s=String::new();
//     println!("Please enter some text: ");
//     let _=stdout().flush();
//     stdin().read_line(&mut s).expect("Did not enter a correct string");
//     s
// }

fn input_user_line()->(std::string::String){
    let args: Vec<String> = env::args().collect();
    let mut string: String = args[2].clone();
    // std::io::stdin().read_line(&mut string);
    string
}


// use std::str::FromStr;

// pub fn Converting(&str)->((amcl_wrapper::field_elem::FieldElement, (amcl_wrapper::group_elem_g1::G1, amcl_wrapper::group_elem_g1::G1),amcl_wrapper::extension_field_gt::GT)){

// }

// fn test<T: FromStr>(text: &str) -> T {
//     text.parse::<T>().expect("string was invalid!")
// }


// fn test<T: FromStr>(text: &str) -> Result<T, (amcl_wrapper::field_elem::FieldElement, (amcl_wrapper::group_elem_g1::G1, amcl_wrapper::group_elem_g1::G1),amcl_wrapper::extension_field_gt::GT)> {
//     text.parse::<T>()
// }

// fn test<T: FromStr>(text: &str) -> Result<T, T::Err> {
//     text.parse::<T>()
// }


use std::time::{Duration, Instant};


#[test]
fn test_scenario_1() {

    let mut stringzz=input_user_line();
    let mut split= stringzz.split(',');
    let vec_input = split.collect::<Vec<&str>>();

    if (vec_input[0]=="test_sen"){
        // Vec<(usize,amcl_wrapper::group_elem_g1::G1,Signature,amcl_wrapper::group_elem_g2::G2,DefaultHasher)>
        let mut gmsk_array=Vec::new();
        //Group Created
        //number of messages used to generate pk and sk
        let count_msgs = 1;
        let label="test".as_bytes();
        let (gpk, gmsk) = GSetup(count_msgs,label);

        // User A Created
        let (upk_1, usk_1)=PKIJoin(count_msgs,label);
        let user_id=1;



        // check DS Encryption
        // let msg = FieldElementVector::random(count_msgs);


        // let start5 = Instant::now();
        // let sign_usk_1=Signature::new(msg.as_slice(), &usk_1, &upk_1).unwrap();
        // let duration5 = start5.elapsed();
        // println!("Time elapsed in DSsigning of A is: {:?}", duration5);


        // let start6 = Instant::now();
        // let check=sign_usk_1.verify(msg.as_slice(),&upk_1.clone()).unwrap();
        // let duration6 = start6.elapsed();
        // println!("Time elapsed in DSverifying of A is: {:?}", duration6);
        // check DS Encryption




        // let mut stringzz= input_user();
        // println!("You typed: {}",stringzz);


        let (secret_register_1,gsk_1) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_1,usk_1);
        //Store A idenity in secret GM array
        gmsk_array.push(secret_register_1.clone());


        // User B Created
        let (upk_2, usk_2)=PKIJoin(count_msgs,label);
        let user_id=2;


        // check DS Encryption
        // let start7 = Instant::now();
        // let sign_usk_2=Signature::new(msg.as_slice(), &usk_2, &upk_2).unwrap();
        // let duration7 = start7.elapsed();
        // println!("Time elapsed in DSsigning of A is: {:?}", duration7);


        // let start8 = Instant::now();
        // let check=sign_usk_2.verify(msg.as_slice(), &upk_2.clone()).unwrap();
        // let duration8 = start8.elapsed();
        // println!("Time elapsed in DSverifying of A is: {:?}", duration8);
        // check DS Encryption




        let (secret_register_2,gsk_2) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_2,usk_2);
        //Store B idenity in secret GM array
        gmsk_array.push(secret_register_2.clone());


        // // Test increase in group size
        // let (upk_3, usk_3)=PKIJoin(count_msgs,label);
        // let user_id=3;
        // let (secret_register_3,gsk_3) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_3,usk_3);
        // gmsk_array.push(secret_register_3.clone());

        // let (upk_4, usk_4)=PKIJoin(count_msgs,label);
        // let user_id=4;
        // let (secret_register_4,gsk_4) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_4,usk_4);
        // gmsk_array.push(secret_register_4.clone());

        // let (upk_5, usk_5)=PKIJoin(count_msgs,label);
        // let user_id=5;
        // let (secret_register_5,gsk_5) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_5,usk_5);
        // gmsk_array.push(secret_register_5.clone());

        // let (upk_6, usk_6)=PKIJoin(count_msgs,label);
        // let user_id=6;
        // let (secret_register_6,gsk_6) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_6,usk_6);
        // gmsk_array.push(secret_register_6.clone());


        // let (upk_7, usk_7)=PKIJoin(count_msgs,label);
        // let user_id=7;
        // let (secret_register_7,gsk_7) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_7,usk_7);
        // gmsk_array.push(secret_register_7.clone());


        // let (upk_8, usk_8)=PKIJoin(count_msgs,label);
        // let user_id=8;
        // let (secret_register_8,gsk_8) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_8,usk_8);
        // gmsk_array.push(secret_register_8.clone());


        // let (upk_9, usk_9)=PKIJoin(count_msgs,label);
        // let user_id=9;
        // let (secret_register_9,gsk_9) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_9,usk_9);
        // gmsk_array.push(secret_register_9.clone());

        // let (upk_10, usk_10)=PKIJoin(count_msgs,label);
        // let user_id=10;
        // let (secret_register_10,gsk_10) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_10,usk_10);
        // gmsk_array.push(secret_register_10.clone());

        // let (upk_11, usk_11)=PKIJoin(count_msgs,label);
        // let user_id=11;
        // let (secret_register_11,gsk_11) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_11,usk_11);
        // gmsk_array.push(secret_register_11.clone());


        // let (upk_12, usk_12)=PKIJoin(count_msgs,label);
        // let user_id=12;
        // let (secret_register_12,gsk_12) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_12,usk_12);
        // gmsk_array.push(secret_register_12.clone());

        // let (upk_13, usk_13)=PKIJoin(count_msgs,label);
        // let user_id=13;
        // let (secret_register_13,gsk_13) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_13,usk_13);
        // gmsk_array.push(secret_register_13.clone());

        // let (upk_14, usk_14)=PKIJoin(count_msgs,label);
        // let user_id=14;
        // let (secret_register_14,gsk_14) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_14,usk_14);
        // gmsk_array.push(secret_register_14.clone());   
        
        // let (upk_15, usk_15)=PKIJoin(count_msgs,label);
        // let user_id=15;
        // let (secret_register_15,gsk_15) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_15,usk_15);
        // gmsk_array.push(secret_register_15.clone());

        // let (upk_16, usk_16)=PKIJoin(count_msgs,label);
        // let user_id=16;
        // let (secret_register_16,gsk_16) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_16,usk_16);
        // gmsk_array.push(secret_register_16.clone());








        // let (upk_x, usk_x)=PKIJoin(count_msgs,label);
        // let user_id=x;
        // let (secret_register_x,gsk_x) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_x,usk_x);
        // gmsk_array.push(secret_register_x.clone());





        //User A signs for message
        let start = Instant::now();
        let (mu_1,hash_for_tuple_1, msg_1)= GSign(gsk_1.clone(),"I require 10 boeing 747");
        let duration = start.elapsed();
        println!("Time elapsed in Gsigning of A is: {:?}", duration);

        let start2 = Instant::now();
        let verified_signature_1=GVerify(gpk.clone(),mu_1.clone(),hash_for_tuple_1.clone(), msg_1.clone());
        let duration2 = start2.elapsed();
        println!("Time elapsed in Gverifying of A is: {:?}", duration2);

        //User B signs for message
        let start3 = Instant::now();
        let (mu_2,hash_for_tuple_2, msg_2)= GSign(gsk_2.clone(),"I require 5 boeing 747");
        let duration3 = start3.elapsed();
        println!("Time elapsed in Gsigning of B is: {:?}", duration3);

        let start4 = Instant::now();
        let verified_signature_2=GVerify(gpk.clone(),mu_2.clone(),hash_for_tuple_2.clone(), msg_2.clone());
        let duration4 = start4.elapsed();
        println!("Time elapsed in Gverifying of B is: {:?}", duration4);


        // //User B signs for message
        // let start5 = Instant::now();
        // let (mu_6,hash_for_tuple_6, msg_6)= GSign(gsk_6.clone(),"I require 15 boeing 747");
        // let duration5 = start5.elapsed();
        // println!("Time elapsed in Gsigning of Z is: {:?}", duration5);

        // let start6 = Instant::now();
        // let verified_signature_6=GVerify(gpk.clone(),mu_6.clone(),hash_for_tuple_6.clone(), msg_6.clone());
        // let duration6 = start6.elapsed();
        // println!("Time elapsed in Gverifying of Z is: {:?}", duration6);





        // who signed mu_1,hash_for_tuple_1,msg_1?
        println!("Who signed this? {:?}", msg_1.clone());
        GOpen(gpk.clone(),gmsk_array.clone(),mu_1.clone(),hash_for_tuple_1.clone(),msg_1.clone());
        // who signed mu_2,hash_for_tuple_2,msg_2?
        println!("Who signed this? {:?}", msg_2.clone());
        GOpen(gpk.clone(),gmsk_array.clone(),mu_2.clone(),hash_for_tuple_2.clone(),msg_2.clone());






    }









    // //Vec<(usize,amcl_wrapper::group_elem_g1::G1,Signature,amcl_wrapper::group_elem_g2::G2,DefaultHasher)>
    // let mut gmsk_array=Vec::new();
    //Group Created
    //number of messages used to generate pk and sk
    // let count_msgs = 1;
    // let label="test".as_bytes();
    // let (gpk, gmsk) = GSetup(count_msgs,label);





    // println!("You typed: {}",vec_input[1]);


    if (vec_input[0]=="GSign"){
        // println!("group_secret_key{:?}", vec_input[1].replace("comma", ","));
        // println!("group_public_key{:?}", vec_input[2].replace("comma", ","));
        // println!("seralization{:?}", vec_input[3].replace("comma", ","));
        let group_secret_key = vec_input[1].replace("comma", ",");
        let group_public_key = vec_input[2].replace("comma", ",");
        let seralization = vec_input[3].replace("comma", ",");

        // let parsed: i32 = "5".parse().unwrap();
        // let turbo_parsed = "10".parse::<i32>().unwrap();

        // let parsed: FieldElement = group_secret_key.parse().unwrap();

        // struct User {
        //     fingerprint: String,
        //     location: String,
        // }
        // // let firstpart: amcl_wrapper::field_elem::FieldElement = serde_json::from_str(group_secret_key).unwrap();
        // let j = "
        //     {
        //         \"fingerprint\": \"0xF9BA143B95FF6D82\",
        //         \"location\": \"Menlo Park, CA\"
        //     }";

        // let u: User = serde_json::from_str(j).unwrap();
        // println!("{:#?}", u);

        // let gsk_string=vec_input[1].split(',');

        // // amcl_wrapper::field_elem::FieldElement
        // let string_1="FieldElement{value: BIG: [0000000000000000000000000000000030313950CD9853C985B0F474BFB2C82DFA2900EE0AE48DA87768D1D88A07B5BE]}";
        // let firstpart: amcl_wrapper::field_elem::FieldElement = serde_json::from_str(string_1).unwrap();
        
        // // (amcl_wrapper::group_elem_g1::G1, amcl_wrapper::group_elem_g1::G1)
        // let string_21="G1{value:ECP:[FP:[BIG:[0F14C406809B10B4C15144BB87CF0BD1017273A17FCC10107F3C8DFDA475195AD8150539A58030706C56BFBE308DC589]],FP:[BIG:[00224B1552B3263C88E87B320DD5F8A42BEE25680CDED3C068495C70E64CC854AA214BECD05CFBDAAA6FB94ABC346D31]],FP:[BIG:[13317C30F3A0D636D56A23C34FDD80B891ECBDE7C2B7D6E16B0F4B0B7E6D26CB6147ACDE629C4A23C57400D203A9FB84]]]}";
        // let string_22="G1{value:ECP:[FP:[BIG:[321A221AD4CBA9A3A6941B72C4DA686DECDD7170B9F06E42F5871661B5A18E2FEE815A1D5FA128D0D70DDE3AAB021754]],FP:[BIG:[2C7B443CDA1EEC37C17AEA4D623E40E1AE17B240345D36CC57FD0EBAEF874DE9BFBB141D8D7A128EF754F04AFA8B1C22]],FP:[BIG:[2F4311D68E7ED02064B0D9BEE9E28D869D4104135220A9E28FB4902DAE3D87B0E97DA23DC7B63B3F76CF030C7F21B6D9]]]}";
        // let secondpart_1 : amcl_wrapper::group_elem_g1::G1 = serde_json::from_str(string_21).unwrap();
        // let secondpart_2 : amcl_wrapper::group_elem_g1::G1 = serde_json::from_str(string_22).unwrap();
        // let secondpart=(secondpart_1,secondpart_2);

        // // amcl_wrapper::extension_field_gt::GT
        // let string_3="[[[008046404E728D91F5E13581802561FECB6EC32DF1437423EEDFC1BF65E53CBB027C06A70FADBC720D6722AE7FB0D4C0,01509FDD094F3EEA55FA9675DEA72DD3EF06C8AAE274F53EB2E99F3D36578A6538A5007655AB4FC52C0CF623345AA133],[0181D9522DAA6B05078EA94852A586FE531D04DE3948072E9095D300FC9BB2B3D619FB89BBFBAA7926EEB0C9D127FAA6,0FEBB21B9E14F9A6FFE3EE874025D470607E0ED024A511197D0E653F3EDA97B4EBE942EC8F82A98CDF76C0418FE05B44]],[[10CEB954A243B245EBFD05CECEBFB8FABE61C711826412D68D7DB0D1EDA4CC8EBB6F0DE40D7F6A381E4A88C650F288DF,184715E1684D10152B53A2EC41ED7FB5CA731B6191EDEE8D0099534C85DF1C64B26A03967C708BD42B1AD67CC0AE1F8F],[19EBBD2A2FD9115CD2DC5CB1BA8F030537C26661F8F995182CAC0A4C05145A228FACCE62DE77B7A786A4107CAF72BF8C,083BB801C161CC12BED81E852095532BA691DE2D14E54BA911EA867E721825A8827AA911DA9A0956E5887693D1868383]],[[08EA96BF7FEC50E2DA40739B7062D3EF3C4FA00CC7ABCA2BCF840270FFB582CE549E62ADA1E606D4D8271B9CFAF67532,1780FB5C14F6A8B875FA72101B1955B5E142A11AD2E5B923A06D03584D4384A2CC4B15B8A6AD10252FC65C443F76CD15],[014DB4C8CB942210152739B4C4416C9109EB9E3FB92527569F4166D478C85A3DCB72F5A2CB0BE9A7959A05B4FF55DB32,19FFF569FF72D009903861BEEC4F911528FDDA7F6467E6335305A93F9F0756925F4C6BA673076C5873449611FE756B5D]]]";
        // let thirdpart: amcl_wrapper::extension_field_gt::GT = serde_json::from_str(string_3).unwrap();

        // let gsk_1 = (firstpart.clone(),secondpart.clone(),thirdpart.clone());
        // // let gsk_1= vec_input[1].parse::<usize>().unwrap();
        // let gpk=vec_input[2].as_bytes();
        // let message=vec_input[3].as_bytes();
        // let (mu_1,hash_for_tuple_1, msg_1)= GSign(gsk_1.clone(),"I require 10 boeing 747");
        // // let verified_signature_1=GVerify(gpk.clone(),mu_1.clone(),hash_for_tuple_1.clone(), msg_1.clone());
        // println!("Your signature is: {:?} ",(mu_1,hash_for_tuple_1, msg_1));
    }


    if (vec_input[0]=="GJoin"){

        let count_msgs = 1;
        let label="test".as_bytes();
        let (gpk, gmsk) = GSetup(count_msgs,label);

        //temp fix
        let count_msgs2 = 1;
        let label2="test".as_bytes();
        let (upk_1, usk_1)=PKIJoin(count_msgs2,label2);
        //arg
        // let user_id=1;
        let user_id=vec_input[1].parse::<usize>().unwrap();

        let (secret_register_1,gsk_1) = GJoin (user_id,gpk.clone(),gmsk.clone(), upk_1,usk_1);
        
        println!("Your secret: {:?} ",gsk_1);
        println!("Group Public: {:?} ",gpk.clone());

    }

    if (vec_input[0]=="PKIJoin"){
        //temp fix
        let count_msgs = 1;
        let label="test".as_bytes();

        let (upk_1, usk_1)=PKIJoin(count_msgs,label);
        //arg
        // let user_id=1;
        let label=vec_input[1].as_bytes();;
        let (upk_1, usk_1)=PKIJoin(count_msgs,label);
        println!("Your secret: {:?} ",usk_1);
    }



}
